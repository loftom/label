package com.medlabel.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.medlabel.dto.AnnotationCreateRequest;
import com.medlabel.entity.AnnotationEntity;
import com.medlabel.mapper.AnnotationMapper;
import com.medlabel.service.LabelMeService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/annotations")
public class AnnotationController {
    private final AnnotationMapper annotationMapper;
    private final LabelMeService labelMeService;

    public AnnotationController(AnnotationMapper annotationMapper, LabelMeService labelMeService) {
        this.annotationMapper = annotationMapper;
        this.labelMeService = labelMeService;
    }

    @PostMapping
    public AnnotationEntity create(@Valid @RequestBody AnnotationCreateRequest request) throws IOException {
        LambdaQueryWrapper<AnnotationEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AnnotationEntity::getImageId, request.getImageId())
                .orderByDesc(AnnotationEntity::getVersion)
                .last("limit 1");
        AnnotationEntity last = annotationMapper.selectOne(wrapper);
        int version = last == null ? 1 : (last.getVersion() == null ? 1 : last.getVersion() + 1);

        Map<String, Integer> counts = labelMeService.countLabels(request.getJsonBody());
        String summary = labelMeService.summarize(counts);

        AnnotationEntity entity = new AnnotationEntity();
        entity.setImageId(request.getImageId());
        entity.setAnnotator(request.getAnnotator());
        entity.setJsonBody(request.getJsonBody());
        entity.setLabelSummary(summary);
        entity.setVersion(version);
        entity.setCreatedAt(LocalDateTime.now());
        annotationMapper.insert(entity);
        return entity;
    }

    @GetMapping
    public List<AnnotationEntity> list(@RequestParam Long imageId,
                                       @RequestParam(required = false) Integer version) {
        LambdaQueryWrapper<AnnotationEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AnnotationEntity::getImageId, imageId);
        if (version != null) {
            wrapper.eq(AnnotationEntity::getVersion, version);
        }
        wrapper.orderByDesc(AnnotationEntity::getVersion);
        return annotationMapper.selectList(wrapper);
    }
}
