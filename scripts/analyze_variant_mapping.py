"""
分析 variant_to_genotype.csv 中 rsID 一对多映射情况
运行方式: 在项目根目录执行 python scripts/analyze_variant_mapping.py
"""
import csv
from collections import defaultdict

CSV_PATH = 'src/main/resources/variant_to_genotype.csv'

rsid_to_alleles = defaultdict(list)  # (gene, rsid) -> [star_allele, ...]
star_to_rsids = defaultdict(list)    # (gene, star_allele) -> [rsid, ...]

with open(CSV_PATH, encoding='utf-8') as f:
    reader = csv.DictReader(f)
    for row in reader:
        gene = row['gene_symbol']
        rsid = row['rsid']
        star = row['star_allele']
        rsid_to_alleles[(gene, rsid)].append(star)
        star_to_rsids[(gene, star)].append(rsid)

total = len(rsid_to_alleles)
many = {k: v for k, v in rsid_to_alleles.items() if len(v) > 1}

print("=" * 70)
print(f"总 (gene, rsid) 对数: {total}")
print(f"一对多 (gene, rsid) 对数: {len(many)}  ({len(many)/total*100:.1f}%)")
print("=" * 70)
print("\n【一对多 rsID 排行（按映射 allele 数降序）】")
print(f"{'gene':<15} {'rsid':<20} {'allele数':>6}  alleles")
print("-" * 70)
for (gene, rsid), alleles in sorted(many.items(), key=lambda x: -len(x[1])):
    print(f"{gene:<15} {rsid:<20} {len(alleles):>6}  {', '.join(alleles)}")

print("\n【每个 star allele 需要的 rsID 数】")
print(f"{'gene':<15} {'star_allele':<15} {'rsid数':>6}  rsids")
print("-" * 70)
single_rsid_stars = [(g_s, rsids) for g_s, rsids in star_to_rsids.items() if len(rsids) == 1]
multi_rsid_stars = [(g_s, rsids) for g_s, rsids in star_to_rsids.items() if len(rsids) > 1]
print(f"  仅需 1 个 rsID 的 star allele: {len(single_rsid_stars)}")
print(f"  需要 2+ 个 rsID 的 star allele: {len(multi_rsid_stars)}")
print()
for (gene, star), rsids in sorted(multi_rsid_stars, key=lambda x: -len(x[1])):
    print(f"{gene:<15} {star:<15} {len(rsids):>6}  {', '.join(rsids)}")
