package com.medlabel.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.medlabel.dto.StatsResponse;
import com.medlabel.entity.AnnotationEntity;
import com.medlabel.entity.MedicalImage;
import com.medlabel.mapper.AnnotationMapper;
import com.medlabel.mapper.MedicalImageMapper;
import com.medlabel.service.LabelMeService;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.io.StringWriter;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequestMapping("/api/stats")
public class StatsController {
    private final AnnotationMapper annotationMapper;
    private final MedicalImageMapper imageMapper;
    private final LabelMeService labelMeService;

    public StatsController(AnnotationMapper annotationMapper, MedicalImageMapper imageMapper, LabelMeService labelMeService) {
        this.annotationMapper = annotationMapper;
        this.imageMapper = imageMapper;
        this.labelMeService = labelMeService;
    }

    @GetMapping("/labels")
    public StatsResponse stats(@RequestParam(required = false) Long imageId,
                               @RequestParam(required = false) Long patientId,
                               @RequestParam(required = false) String from,
                               @RequestParam(required = false) String to) throws IOException {
        List<AnnotationEntity> annotations = queryAnnotations(imageId, patientId, from, to);
        Map<String, Integer> byLabel = new HashMap<>();
        Map<String, Integer> byAnnotator = new HashMap<>();
        int total = 0;

        for (AnnotationEntity entity : annotations) {
            Map<String, Integer> counts = parseSummary(entity);
            for (Map.Entry<String, Integer> entry : counts.entrySet()) {
                byLabel.put(entry.getKey(), byLabel.getOrDefault(entry.getKey(), 0) + entry.getValue());
                total += entry.getValue();
            }
            String annotator = entity.getAnnotator() == null ? "unknown" : entity.getAnnotator();
            byAnnotator.put(annotator, byAnnotator.getOrDefault(annotator, 0) + 1);
        }

        return new StatsResponse(total, byLabel, byAnnotator);
    }

    @GetMapping("/export")
    public ResponseEntity<String> export(@RequestParam(required = false) Long imageId,
                                         @RequestParam(required = false) Long patientId,
                                         @RequestParam(required = false) String from,
                                         @RequestParam(required = false) String to) throws IOException {
        StatsResponse response = stats(imageId, patientId, from, to);
        StringWriter writer = new StringWriter();
        try (CSVPrinter printer = new CSVPrinter(writer, CSVFormat.DEFAULT.withHeader("label", "count"))) {
            for (Map.Entry<String, Integer> entry : response.getByLabel().entrySet()) {
                printer.printRecord(entry.getKey(), entry.getValue());
            }
        }
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=stats.csv")
                .contentType(MediaType.TEXT_PLAIN)
                .body(writer.toString());
    }

    private List<AnnotationEntity> queryAnnotations(Long imageId, Long patientId, String from, String to) {
        LambdaQueryWrapper<AnnotationEntity> wrapper = new LambdaQueryWrapper<>();
        if (imageId != null) {
            wrapper.eq(AnnotationEntity::getImageId, imageId);
        } else if (patientId != null) {
            LambdaQueryWrapper<MedicalImage> imgWrapper = new LambdaQueryWrapper<>();
            imgWrapper.eq(MedicalImage::getPatientId, patientId);
            List<MedicalImage> images = imageMapper.selectList(imgWrapper);
            List<Long> ids = new ArrayList<>();
            for (MedicalImage image : images) {
                ids.add(image.getId());
            }
            if (ids.isEmpty()) {
                return Collections.emptyList();
            }
            wrapper.in(AnnotationEntity::getImageId, ids);
        }

        LocalDateTime fromTime = parseTime(from);
        LocalDateTime toTime = parseTime(to);
        if (fromTime != null) {
            wrapper.ge(AnnotationEntity::getCreatedAt, fromTime);
        }
        if (toTime != null) {
            wrapper.le(AnnotationEntity::getCreatedAt, toTime);
        }
        wrapper.orderByDesc(AnnotationEntity::getCreatedAt);
        return annotationMapper.selectList(wrapper);
    }

    private LocalDateTime parseTime(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        if (value.contains("T")) {
            return LocalDateTime.parse(value);
        }
        return LocalDate.parse(value).atStartOfDay();
    }

    private Map<String, Integer> parseSummary(AnnotationEntity entity) throws IOException {
        if (entity.getLabelSummary() == null || entity.getLabelSummary().isBlank()) {
            return labelMeService.countLabels(entity.getJsonBody());
        }
        Map<String, Integer> result = new HashMap<>();
        String[] parts = entity.getLabelSummary().split(";");
        for (String part : parts) {
            if (part.isBlank() || !part.contains(":")) {
                continue;
            }
            String[] kv = part.split(":", 2);
            result.put(kv[0], Integer.parseInt(kv[1]));
        }
        return result;
    }
}
