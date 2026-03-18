new Vue({
  el: '#app',
  data() {
    return {
      activeTab: 'patients',
      patients: [],
      patientKeyword: '',
      patientForm: {
        patientNo: '',
        name: '',
        gender: 'U',
        dob: '',
        contact: ''
      },
      uploadForm: {
        patientId: null,
        uploadedBy: ''
      },
      uploadFile: null,
      uploadResult: null,
      images: [],
      imageMgmtFilterPatientId: null,
      imageFilterPatientId: null,
      selectedImage: null,
      imagePreviewUrl: '',
      previewImg: null,
      shapeType: 'rectangle',
      annotator: '',
      shapes: [],
      drawing: false,
      currentPoints: [],
      statsForm: {
        imageId: '',
        patientId: '',
        from: '',
        to: ''
      },
      labelStats: [],
      annotatorStats: []
    };
  },
  mounted() {
    this.loadPatients();
    this.loadImages();
    this.initCanvas();
  },
  methods: {
    async request(url, options) {
      const res = await fetch(url, options);
      if (!res.ok) {
        const text = await res.text();
        this.$message.error(text || '请求失败');
        throw new Error(text || '请求失败');
      }
      if (options && options.responseType === 'text') {
        return res.text();
      }
      return res.json();
    },
    async loadPatients() {
      const url = this.patientKeyword ? `/api/patients?keyword=${encodeURIComponent(this.patientKeyword)}` : '/api/patients';
      this.patients = await this.request(url);
    },
    async createPatient() {
      const payload = Object.assign({}, this.patientForm);
      await this.request('/api/patients', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(payload)
      });
      this.$message.success('患者已创建');
      this.patientForm = { patientNo: '', name: '', gender: 'U', dob: '', contact: '' };
      this.loadPatients();
    },
    handleFile(e) {
      this.uploadFile = e.target.files[0];
    },
    async uploadImage() {
      if (!this.uploadFile) {
        this.$message.warning('请选择文件');
        return;
      }
      const formData = new FormData();
      formData.append('file', this.uploadFile);
      if (this.uploadForm.patientId) formData.append('patientId', this.uploadForm.patientId);
      if (this.uploadForm.uploadedBy) formData.append('uploadedBy', this.uploadForm.uploadedBy);
      this.uploadResult = await this.request('/api/images/upload', { method: 'POST', body: formData });
      this.$message.success('上传成功');
      this.loadImages();
    },
    async loadImages() {
      const params = [];
      // support both management filter and annotate filter
      const filterPid = this.imageMgmtFilterPatientId || this.imageFilterPatientId;
      if (filterPid) params.push(`patientId=${filterPid}`);
      const url = params.length ? `/api/images?${params.join('&')}` : '/api/images';
      const page = await this.request(url);
      this.images = page.records || [];
    },
    previewImage(row) {
      window.open(`/api/images/${row.id}/preview`, '_blank');
    },
    confirmDelete(row) {
      this.$confirm('确认删除该影像吗？此操作不可恢复。', '删除确认', { type: 'warning' })
        .then(() => this.deleteImage(row.id))
        .catch(() => {});
    },
    async deleteImage(id) {
      await this.request(`/api/images/${id}`, { method: 'DELETE' });
      this.$message.success('已删除');
      this.loadImages();
    },
    selectImage(row) {
      this.selectedImage = row;
      this.imagePreviewUrl = `/api/images/${row.id}/preview`;
      this.shapes = [];
      this.currentPoints = [];
      // prepare and cache preview image to avoid reloading on every redraw
      this.previewImg = new Image();
      this.previewImg.crossOrigin = 'anonymous';
      this.previewImg.onload = () => {
        const canvas = document.getElementById('canvas');
        if (!canvas) return;
        if (canvas.width !== this.previewImg.width || canvas.height !== this.previewImg.height) {
          canvas.width = this.previewImg.width;
          canvas.height = this.previewImg.height;
        }
        this.loadAnnotations();
        this.redraw();
      };
      this.previewImg.src = this.imagePreviewUrl;
    },
    async loadAnnotations() {
      if (!this.selectedImage) return;
      try {
        const res = await this.request(`/api/annotations?imageId=${this.selectedImage.id}`);
        if (res && res.length > 0) {
          // take latest (controller orders desc by version)
          const ann = res[0];
          let body = ann.jsonBody;
          if (typeof body === 'string') body = JSON.parse(body);
          this.shapes = (body.shapes || []).map(s => ({
            label: s.label,
            points: (s.points || []).map(p => ({ x: p[0], y: p[1] })),
            shape_type: s.shape_type || s.shapeType || 'polygon'
          }));
        } else {
          this.shapes = [];
        }
      } catch (e) {
        this.$message.error('加载标注失败');
        this.shapes = [];
      }
      this.currentPoints = [];
      this.redraw();
    },
    initCanvas() {
      const canvas = document.getElementById('canvas');
      if (!canvas) return;
      const ctx = canvas.getContext('2d');
      canvas.onmousedown = (e) => this.onMouseDown(e, ctx);
      canvas.onmousemove = (e) => this.onMouseMove(e, ctx);
      canvas.onmouseup = (e) => this.onMouseUp(e, ctx);
      canvas.ondblclick = (e) => this.onDoubleClick(e, ctx);
      this.redraw();
    },
    getCanvasPos(e) {
      const rect = e.target.getBoundingClientRect();
      return { x: e.clientX - rect.left, y: e.clientY - rect.top };
    },
    onMouseDown(e, ctx) {
      if (!this.selectedImage) return;
      const pos = this.getCanvasPos(e);
      if (this.shapeType === 'point') {
        const label = prompt('点的名称', 'point');
        if (!label) return;
        this.shapes.push({ label, points: [pos], shape_type: 'point' });
        this.redraw();
        return;
      }
      if (this.shapeType === 'polygon') {
        this.currentPoints.push(pos);
        this.redraw();
        return;
      }
      if (this.shapeType === 'rectangle') {
        this.drawing = true;
        this.currentPoints = [pos, pos];
      }
    },
    onMouseMove(e, ctx) {
      if (!this.drawing || this.shapeType !== 'rectangle') return;
      const pos = this.getCanvasPos(e);
      this.currentPoints[1] = pos;
      this.redraw();
    },
    onMouseUp(e, ctx) {
      if (this.shapeType !== 'rectangle' || !this.drawing) return;
      this.drawing = false;
      const label = prompt('矩形名称', 'rect');
      if (!label) {
        this.currentPoints = [];
        this.redraw();
        return;
      }
      const p1 = this.currentPoints[0];
      const p2 = this.currentPoints[1];
      const points = [{ x: p1.x, y: p1.y }, { x: p2.x, y: p2.y }];
      this.shapes.push({ label, points, shape_type: 'rectangle' });
      this.currentPoints = [];
      this.redraw();
    },
    onDoubleClick(e, ctx) {
      if (this.shapeType !== 'polygon' || this.currentPoints.length < 3) return;
      const label = prompt('多边形名称', 'polygon');
      if (!label) {
        this.currentPoints = [];
        this.redraw();
        return;
      }
      this.shapes.push({ label, points: this.currentPoints.slice(), shape_type: 'polygon' });
      this.currentPoints = [];
      this.redraw();
    },
    clearShapes() {
      this.shapes = [];
      this.currentPoints = [];
      this.redraw();
    },
    redraw() {
      if (this._rafId) cancelAnimationFrame(this._rafId);
      this._rafId = requestAnimationFrame(() => {
        const canvas = document.getElementById('canvas');
        if (!canvas) return;
        const ctx = canvas.getContext('2d');
        ctx.clearRect(0, 0, canvas.width, canvas.height);
        if (this.previewImg && this.previewImg.complete) {
          if (canvas.width !== this.previewImg.width || canvas.height !== this.previewImg.height) {
            canvas.width = this.previewImg.width;
            canvas.height = this.previewImg.height;
          }
          ctx.drawImage(this.previewImg, 0, 0);
          this.drawShapes(ctx);
        } else {
          // no image available yet, just draw shapes (usually empty)
          this.drawShapes(ctx);
        }
      });
    },
    drawShapes(ctx) {
      ctx.strokeStyle = '#00ff99';
      ctx.fillStyle = 'rgba(255,0,0,0.7)';
      this.shapes.forEach(shape => {
        if (shape.shape_type === 'point') {
          const p = shape.points[0];
          ctx.beginPath();
          ctx.arc(p.x, p.y, 4, 0, Math.PI * 2);
          ctx.fill();
          ctx.fillText(shape.label, p.x + 6, p.y - 6);
        }
        if (shape.shape_type === 'rectangle') {
          const p1 = shape.points[0];
          const p2 = shape.points[1];
          const x = Math.min(p1.x, p2.x);
          const y = Math.min(p1.y, p2.y);
          const w = Math.abs(p2.x - p1.x);
          const h = Math.abs(p2.y - p1.y);
          ctx.strokeRect(x, y, w, h);
          ctx.fillText(shape.label, x + 4, y + 12);
        }
        if (shape.shape_type === 'polygon') {
          ctx.beginPath();
          shape.points.forEach((p, idx) => {
            if (idx === 0) ctx.moveTo(p.x, p.y);
            else ctx.lineTo(p.x, p.y);
          });
          ctx.closePath();
          ctx.stroke();
          const p = shape.points[0];
          ctx.fillText(shape.label, p.x + 4, p.y + 12);
        }
      });
      if (this.currentPoints.length > 0) {
        ctx.strokeStyle = '#ffaa00';
        ctx.beginPath();
        this.currentPoints.forEach((p, idx) => {
          if (idx === 0) ctx.moveTo(p.x, p.y);
          else ctx.lineTo(p.x, p.y);
        });
        ctx.stroke();
      }
    },
    buildLabelMeJson() {
      const canvas = document.getElementById('canvas');
      return {
        version: '5.0.1',
        flags: {},
        shapes: this.shapes.map(shape => ({
          label: shape.label,
          points: shape.points.map(p => [p.x, p.y]),
          shape_type: shape.shape_type,
          flags: {}
        })),
        imagePath: this.selectedImage ? this.selectedImage.fileName : '',
        imageData: null,
        imageHeight: canvas ? canvas.height : 0,
        imageWidth: canvas ? canvas.width : 0
      };
    },
    async saveAnnotation() {
      if (!this.selectedImage) {
        this.$message.warning('请选择影像');
        return;
      }
      const jsonBody = JSON.stringify(this.buildLabelMeJson());
      await this.request('/api/annotations', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          imageId: this.selectedImage.id,
          annotator: this.annotator,
          jsonBody
        })
      });
      this.$message.success('标注已保存');
      // reload latest annotation and show it
      await this.loadAnnotations();
    },
    async loadStats() {
      const params = [];
      if (this.statsForm.imageId) params.push(`imageId=${this.statsForm.imageId}`);
      if (this.statsForm.patientId) params.push(`patientId=${this.statsForm.patientId}`);
      if (this.statsForm.from) params.push(`from=${this.statsForm.from}`);
      if (this.statsForm.to) params.push(`to=${this.statsForm.to}`);
      const url = `/api/stats/labels${params.length ? '?' + params.join('&') : ''}`;
      const res = await this.request(url);
      this.labelStats = Object.keys(res.byLabel || {}).map(label => ({ label, count: res.byLabel[label] }));
      this.annotatorStats = Object.keys(res.byAnnotator || {}).map(annotator => ({ annotator, count: res.byAnnotator[annotator] }));
    },
    exportCsv() {
      const params = [];
      if (this.statsForm.imageId) params.push(`imageId=${this.statsForm.imageId}`);
      if (this.statsForm.patientId) params.push(`patientId=${this.statsForm.patientId}`);
      if (this.statsForm.from) params.push(`from=${this.statsForm.from}`);
      if (this.statsForm.to) params.push(`to=${this.statsForm.to}`);
      const url = `/api/stats/export${params.length ? '?' + params.join('&') : ''}`;
      window.open(url, '_blank');
    }
  }
});
