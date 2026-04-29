-- 先创建一个测试 sample
INSERT INTO sample (id, created_at, uploaded_by, matching_status)
VALUES (999, NOW(), 'test_gt_v4.1', 'pending');

-- CYP2C19 rs4244285 GT=1|1 (纯合变异)
-- Otherinfo 包含 GT 信息
INSERT INTO annovar (
    sample_id, Chr, Start, End, Ref, Alt,
    `Func.refGene`, `Gene.refGene`, `GeneDetail.refGene`, `ExonicFunc.refGene`, `AAChange.refGene`,
    cytoBand, avsnp150,
    Otherinfo
) VALUES (
    999, 'chr10', '94781859', '94781859', 'G', 'A',
    'exonic', 'CYP2C19', '.', 'nonsynonymous SNV', 'CYP2C19:NM_000769:exon5:c.G681A',
    '10q24.1', 'rs4244285',
    'GT:AD:DP:GQ	1|1:0,4:4:99'
);

-- CYP2C19 rs3758581 GT=1|1 (纯合变异)
INSERT INTO annovar (
    sample_id, Chr, Start, End, Ref, Alt,
    `Func.refGene`, `Gene.refGene`, `GeneDetail.refGene`, `ExonicFunc.refGene`, `AAChange.refGene`,
    cytoBand, avsnp150,
    Otherinfo
) VALUES (
    999, 'chr10', '94842866', '94842866', 'A', 'G',
    'exonic', 'CYP2C19', '.', 'nonsynonymous SNV', 'CYP2C19:NM_000769:exon9:c.A991G',
    '10q24.1', 'rs3758581',
    'GT:AD:DP:GQ	1|1:0,2:2:99'
);

-- CYP2C9 rs1799853 GT=0|1 (杂合)
INSERT INTO annovar (
    sample_id, Chr, Start, End, Ref, Alt,
    `Func.refGene`, `Gene.refGene`, `GeneDetail.refGene`, `ExonicFunc.refGene`, `AAChange.refGene`,
    cytoBand, avsnp150,
    Otherinfo
) VALUES (
    999, 'chr10', '94942290', '94942290', 'C', 'T',
    'exonic', 'CYP2C9', '.', 'nonsynonymous SNV', 'CYP2C9:NM_000771:exon3:c.C430T',
    '10q24.1', 'rs1799853',
    'GT:AD:DP:GQ	0|1:2,1:3:30'
);

-- 验证插入
SELECT sample_id, `Gene.refGene`, avsnp150, Otherinfo FROM annovar WHERE sample_id = 999;