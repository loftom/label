package com.medlabel.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.medlabel.entity.MedicalImage;
import com.medlabel.mapper.MedicalImageMapper;
import com.medlabel.mapper.PatientMapper;
import com.medlabel.dto.MedicalImageDto;
import com.medlabel.entity.Patient;
import com.medlabel.service.DicomService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/images")
public class ImageController {
    private final MedicalImageMapper imageMapper;
    private final PatientMapper patientMapper;
    private final DicomService dicomService;

    @Value("${app.storage-path:uploads}")
    private String storagePath;

    public ImageController(MedicalImageMapper imageMapper, PatientMapper patientMapper, DicomService dicomService) {
        this.imageMapper = imageMapper;
        this.patientMapper = patientMapper;
        this.dicomService = dicomService;
    }

    @PostMapping("/upload")
    public Map<String, Object> upload(@RequestParam(required = false) Long patientId,
                                      @RequestParam(required = false) String uploadedBy,
                                      @RequestPart("file") MultipartFile file) throws IOException {
        String originalName = file.getOriginalFilename();
        String ext = originalName != null && originalName.contains(".")
                ? originalName.substring(originalName.lastIndexOf('.')).toLowerCase()
                : "";
        String datePath = LocalDate.now().toString();
        Path dir = Path.of(storagePath, datePath);
        Files.createDirectories(dir);
        String filename = UUID.randomUUID().toString().replace("-", "") + ext;
        Path target = dir.resolve(filename);
        file.transferTo(target);

        MedicalImage image = new MedicalImage();
        image.setPatientId(patientId);
        image.setFileName(originalName);
        // store absolute path so file remains accessible after restarts regardless of working dir
        image.setFilePath(target.toAbsolutePath().toString());
        image.setUploadedBy(uploadedBy);
        image.setUploadedAt(LocalDateTime.now());

        if (".dcm".equals(ext)) {
            DicomService.DicomMeta meta = dicomService.readMeta(target.toFile());
            image.setModality(meta.getModality());
            image.setStudyUid(meta.getStudyUid());
            image.setSeriesUid(meta.getSeriesUid());
            image.setInstanceNumber(meta.getInstanceNumber());
        }

        imageMapper.insert(image);

        Map<String, Object> resp = new HashMap<>();
        resp.put("imageId", image.getId());
        resp.put("fileName", image.getFileName());
        return resp;
    }

    @GetMapping
    public Page<MedicalImageDto> list(@RequestParam(required = false) Long patientId,
                                       @RequestParam(defaultValue = "1") int page,
                                       @RequestParam(defaultValue = "10") int size) {
        LambdaQueryWrapper<com.medlabel.entity.MedicalImage> wrapper = new LambdaQueryWrapper<>();
        if (patientId != null) {
            wrapper.eq(com.medlabel.entity.MedicalImage::getPatientId, patientId);
        }
        wrapper.orderByDesc(com.medlabel.entity.MedicalImage::getUploadedAt);
        Page<com.medlabel.entity.MedicalImage> raw = imageMapper.selectPage(new Page<>(page, size), wrapper);
        Page<MedicalImageDto> out = new Page<>(raw.getCurrent(), raw.getSize(), raw.getTotal());
        out.setRecords(raw.getRecords().stream().map(r -> {
            MedicalImageDto dto = new MedicalImageDto();
            dto.setId(r.getId());
            dto.setPatientId(r.getPatientId());
            if (r.getPatientId() != null) {
                Patient p = patientMapper.selectById(r.getPatientId());
                dto.setPatientName(p != null ? p.getName() : null);
            }
            dto.setFileName(r.getFileName());
            dto.setFilePath(r.getFilePath());
            dto.setModality(r.getModality());
            dto.setStudyUid(r.getStudyUid());
            dto.setSeriesUid(r.getSeriesUid());
            dto.setInstanceNumber(r.getInstanceNumber());
            dto.setUploadedBy(r.getUploadedBy());
            dto.setUploadedAt(r.getUploadedAt());
            return dto;
        }).toList());
        return out;
    }

    @GetMapping("/{id}/preview")
    public ResponseEntity<Resource> preview(@PathVariable Long id) throws IOException {
        MedicalImage image = imageMapper.selectById(id);
        if (image == null) {
            return ResponseEntity.notFound().build();
        }
        File file = new File(image.getFilePath());
        if (!file.exists()) {
            return ResponseEntity.notFound().build();
        }
        if (image.getFileName() != null && image.getFileName().toLowerCase().endsWith(".dcm")) {
            return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE).build();
        }
        FileSystemResource resource = new FileSystemResource(file);
        String contentType = Files.probeContentType(file.toPath());
        MediaType mediaType = contentType != null ? MediaType.parseMediaType(contentType) : MediaType.APPLICATION_OCTET_STREAM;
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + image.getFileName() + "\"")
                .contentType(mediaType)
                .body(resource);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> delete(@PathVariable Long id) {
        MedicalImage image = imageMapper.selectById(id);
        if (image == null) {
            return ResponseEntity.notFound().build();
        }
        File file = new File(image.getFilePath());
        Map<String, Object> resp = new HashMap<>();
        boolean fileDeleted = true;
        if (file.exists()) {
            try {
                fileDeleted = file.delete();
            } catch (Exception e) {
                fileDeleted = false;
            }
        }
        imageMapper.deleteById(id);
        resp.put("deleted", true);
        resp.put("fileDeleted", fileDeleted);
        return ResponseEntity.ok(resp);
    }
}
