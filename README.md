# DST Web Application

DST Web Application - 

## 版本更新记录

### 最新进展

- **染色体感知处理功能**：实现了基于VCF GT字段的精确杂合子/纯合子识别的V4.1 pipeline
- **完善打分策略**：优化了V2星等位基因打分策略，提高匹配准确性
- **多基因支持**：已实现CYP2C9和CYP2C19等多个基因的同时处理
- **批处理优化**：修复了AnnovarDao中的批处理bug，确保数据完整性
- **数据库优化**：增加了基因型-表型关联映射表，提升查询效率

### 功能模块

- **核心匹配流程**：rsID → 星等位基因 → 二倍体型 → 表型 → 用药指导
- **数据管理**：支持variant2genotype和genotype2phenotype数据映射
- **结果分类**：代谢物匹配与一般匹配结果分别展示

### merge记录
- chenzi分支4.29merge到main