#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
PharmCAT Data Extractor
Extract data from PharmCAT JSON files to CSV

Output:
  - variant_to_genotype.csv
  - genotype_to_phenotype.csv
"""

import json
import os
import csv

# PharmCAT data paths
PHARMCAT_ALLELES_DIR = r"E:\openclaw_workspace\PharmCAT-development\PharmCAT-development\src\main\resources\org\pharmgkb\pharmcat\definition\alleles"
PHARMCAT_PHENOTYPE_DIR = r"E:\openclaw_workspace\PharmCAT-development\PharmCAT-development\src\main\resources\org\pharmgkb\pharmcat\phenotype"

# Output path (DST_webapp resources directory)
OUTPUT_DIR = r"D:\OneDrive - International Campus, Zhejiang University\DST2\WebAPP\DST_webapp\src\main\resources"

# Core PGx gene list (CPIC guidelines)
CORE_PGX_GENES = [
    'CYP2C19', 'CYP2C9', 'CYP2D6', 'CYP3A4', 'CYP3A5', 'CYP2B6',
    'TPMT', 'NUDT15', 'DPYD', 'UGT1A1', 'SLCO1B1', 'VKORC1', 'ABCG2'
]


def extract_variant_to_genotype():
    """
    Extract variant_to_genotype data from alleles/*.json
    """
    print("=" * 60)
    print("Extracting variant_to_genotype data...")
    print("=" * 60)
    
    rows = []
    
    for gene in CORE_PGX_GENES:
        allele_file = os.path.join(PHARMCAT_ALLELES_DIR, f"{gene}_translation.json")
        
        if not os.path.exists(allele_file):
            print(f"  [SKIP] {gene} (file not found)")
            continue
        
        print(f"  [INFO] Processing {gene}...")
        
        with open(allele_file, 'r', encoding='utf-8') as f:
            data = json.load(f)
        
        chromosome = data.get('chromosome', '')
        
        # Build position -> rsid mapping from variants array
        pos_to_rsid = {}
        pos_to_ref = {}
        for variant in data.get('variants', []):
            pos = str(variant.get('position'))
            rsid = variant.get('rsid')
            ref = variant.get('ref', '')
            if pos and rsid:
                pos_to_rsid[pos] = rsid
                pos_to_ref[pos] = ref
        
        # Extract from positionToAlleles mapping
        position_to_alleles = data.get('positionToAlleles', {})
        
        for position, alleles in position_to_alleles.items():
            rsid = pos_to_rsid.get(position, '')
            ref = pos_to_ref.get(position, '')
            
            if not rsid:
                continue  # Skip if no rsID
            
            # Each position can map to multiple alleles
            for allele_name in alleles:
                if allele_name == '*1':
                    continue  # Skip reference allele
                
                rows.append({
                    'gene_symbol': gene,
                    'rsid': rsid,
                    'chromosome': chromosome,
                    'position': position,
                    'ref_allele': ref,
                    'alt_allele': '',  # Will be filled later
                    'star_allele': allele_name,
                    'allele_function': '',
                    'is_required': 'TRUE'
                })
    
    # Write CSV
    output_file = os.path.join(OUTPUT_DIR, 'variant_to_genotype.csv')
    
    if rows:
        with open(output_file, 'w', newline='', encoding='utf-8') as f:
            fieldnames = ['gene_symbol', 'rsid', 'chromosome', 'position', 
                         'ref_allele', 'alt_allele', 'star_allele', 
                         'allele_function', 'is_required']
            writer = csv.DictWriter(f, fieldnames=fieldnames)
            writer.writeheader()
            writer.writerows(rows)
        
        print(f"\n[SUCCESS] Generated {output_file}")
        print(f"  Total rows: {len(rows)}")
    else:
        print(f"\n[ERROR] No data extracted")
    
    return rows


def extract_genotype_to_phenotype():
    """
    Extract genotype_to_phenotype data from phenotype/*.json
    """
    print("\n" + "=" * 60)
    print("Extracting genotype_to_phenotype data...")
    print("=" * 60)
    
    rows = []
    
    for gene in CORE_PGX_GENES:
        phenotype_file = os.path.join(PHARMCAT_PHENOTYPE_DIR, f"{gene}.json")
        
        if not os.path.exists(phenotype_file):
            print(f"  [SKIP] {gene} (file not found)")
            continue
        
        print(f"  [INFO] Processing {gene}...")
        
        with open(phenotype_file, 'r', encoding='utf-8') as f:
            data = json.load(f)
        
        # Extract diplotype -> phenotype mapping
        for diplotype_entry in data.get('diplotypes', []):
            diplotype = diplotype_entry.get('diplotype', '')
            phenotype = diplotype_entry.get('phenotype', '')
            generesult = diplotype_entry.get('generesult', '')
            
            # Calculate activity score (if applicable)
            activity_score = calculate_activity_score(gene, diplotype_entry)
            
            # Get function category
            function_category = generesult
            
            rows.append({
                'gene_symbol': gene,
                'diplotype': diplotype,
                'phenotype': phenotype,
                'activity_score': activity_score,
                'function_category': function_category
            })
    
    # Write CSV
    output_file = os.path.join(OUTPUT_DIR, 'genotype_to_phenotype.csv')
    
    if rows:
        with open(output_file, 'w', newline='', encoding='utf-8') as f:
            fieldnames = ['gene_symbol', 'diplotype', 'phenotype', 
                         'activity_score', 'function_category']
            writer = csv.DictWriter(f, fieldnames=fieldnames)
            writer.writeheader()
            writer.writerows(rows)
        
        print(f"\n[SUCCESS] Generated {output_file}")
        print(f"  Total rows: {len(rows)}")
    else:
        print(f"\n[ERROR] No data extracted")
    
    return rows


def calculate_activity_score(gene, diplotype_entry):
    """
    Calculate activity score (mainly for CYP2C9, CYP2D6)
    """
    activity_values = {
        'CYP2C9': {'*1': 1.0, '*2': 0.5, '*3': 0.0},
        'CYP2D6': {'*1': 1.0, '*2': 1.0, '*3': 0.0, '*4': 0.0, '*5': 0.0, '*10': 0.5},
    }
    
    if gene not in activity_values:
        return ''
    
    allele_counts = diplotype_entry.get('diplotypekey', {})
    score = 0.0
    
    for allele, count in allele_counts.items():
        allele_score = activity_values[gene].get(allele, 1.0)
        score += allele_score * count
    
    return round(score, 1) if score else ''


def main():
    print("\n" + "=" * 60)
    print("PharmCAT Data Extractor")
    print("=" * 60)
    print(f"\nPharmCAT Alleles Directory: {PHARMCAT_ALLELES_DIR}")
    print(f"PharmCAT Phenotype Directory: {PHARMCAT_PHENOTYPE_DIR}")
    print(f"Output Directory: {OUTPUT_DIR}")
    print(f"Processing Genes: {len(CORE_PGX_GENES)}")
    print()
    
    # Ensure output directory exists
    os.makedirs(OUTPUT_DIR, exist_ok=True)
    
    # Extract data
    variant_rows = extract_variant_to_genotype()
    phenotype_rows = extract_genotype_to_phenotype()
    
    # Print summary
    print("\n" + "=" * 60)
    print("Extraction Summary")
    print("=" * 60)
    print(f"variant_to_genotype.csv: {len(variant_rows)} rows")
    print(f"genotype_to_phenotype.csv: {len(phenotype_rows)} rows")
    print(f"\nOutput Files:")
    print(f"  - {os.path.join(OUTPUT_DIR, 'variant_to_genotype.csv')}")
    print(f"  - {os.path.join(OUTPUT_DIR, 'genotype_to_phenotype.csv')}")
    print()


if __name__ == '__main__':
    main()
