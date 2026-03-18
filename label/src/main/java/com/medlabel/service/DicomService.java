package com.medlabel.service;

import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

@Service
public class DicomService {
    /**
     * 简单检查 DICOM 文件头（在偏移 128 存在 "DICM" 标识），不依赖外部库。
     * 返回的元数据尽量填充基本信息，复杂字段建议后续用 dcm4che 完整解析。
     */
    public DicomMeta readMeta(File file) throws IOException {
        DicomMeta meta = new DicomMeta();
        if (file == null || !file.exists()) return meta;
        try (FileInputStream in = new FileInputStream(file)) {
            if (in.available() > 132) {
                byte[] header = new byte[132];
                int read = in.read(header);
                if (read == 132 && header[128] == 'D' && header[129] == 'I' && header[130] == 'C' && header[131] == 'M') {
                    meta.setModality("DICOM");
                }
            }
        }
        return meta;
    }

    public static class DicomMeta {
        private String modality;
        private String studyUid;
        private String seriesUid;
        private Integer instanceNumber;

        public String getModality() {
            return modality;
        }

        public void setModality(String modality) {
            this.modality = modality;
        }

        public String getStudyUid() {
            return studyUid;
        }

        public void setStudyUid(String studyUid) {
            this.studyUid = studyUid;
        }

        public String getSeriesUid() {
            return seriesUid;
        }

        public void setSeriesUid(String seriesUid) {
            this.seriesUid = seriesUid;
        }

        public Integer getInstanceNumber() {
            return instanceNumber;
        }

        public void setInstanceNumber(Integer instanceNumber) {
            this.instanceNumber = instanceNumber;
        }
    }
}
