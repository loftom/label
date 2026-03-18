# 医疗影像辅助分析系统（作业版）

轻量级医疗影像标注系统，包含：患者信息管理、影像上传与存储、影像浏览与标注（矩形/多边形/点，LabelMe JSON）、标注统计与 CSV 导出。

## 功能概览
- 患者管理：创建与查询
- 影像上传：单文件上传，支持 DICOM 元数据解析
- 影像标注：前端 Canvas 标注，保存 LabelMe JSON
- 统计分析：按标签/影像/患者/标注人计数，支持时间区间，CSV 导出

## 技术栈
- 后端：Java 17，Spring Boot，MyBatis-Plus
- 数据库：MySQL
- 前端：Vue2 + Element-UI（CDN）
- 构建：Maven

## 数据库初始化
执行 [schema.sql](schema.sql) 创建数据库与表。

## 配置说明
在 [src/main/resources/application.yml](src/main/resources/application.yml) 中修改数据库账号与密码。

## 运行方式
```bash
mvn -q -DskipTests spring-boot:run
```
启动后打开：
```
http://localhost:8080
```

## 核心接口
- POST /api/patients
- GET /api/patients?keyword=
- POST /api/images/upload
- GET /api/images/{id}/preview
- GET /api/images?patientId=&page=&size=
- POST /api/annotations
- GET /api/annotations?imageId=&version=
- GET /api/stats/labels?imageId=&patientId=&from=&to=
- GET /api/stats/export?imageId=&patientId=&from=&to=
