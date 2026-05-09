create table annovar
(
    sample_id int not null,
    Chr varchar(100) not null,
    Start varchar(100) not null,
    End varchar(100) not null,
    Ref varchar(100) not null,
    Alt varchar(100) not null,
    Func_refGene text null,
    Gene_refGene text null,
    GeneDetail_refGene text null,
    ExonicFunc_refGene text null,
    AAChange_refGene text null,
    rsID varchar(100) null,
    GT varchar(50) null,
    primary key (sample_id, Chr, Start, End, Ref, Alt)
);

create table dosing_guideline
(
    id varchar(100) not null,
    obj_cls varchar(100) null,
    name varchar(100) null,
    recommendation tinyint(1) null,
    drug_id varchar(100) null,
    source varchar(100) null,
    summary_markdown varchar(2000) null,
    text_markdown text null,
    raw longtext null,
    constraint dosing_guideline_id_uindex
        unique (id)
);

alter table dosing_guideline
    add primary key (id);

create table drug
(
    id varchar(100) not null,
    name varchar(500) null,
    obj_cls varchar(100) null,
    drug_url varchar(100) null,
    biomarker tinyint(1) null,
    constraint drug_id_uindex
        unique (id)
);

alter table drug
    add primary key (id);

create table drug_label
(
    id varchar(100) not null,
    name varchar(200) null,
    obj_cls varchar(100) null,
    alternate_drug_available tinyint(1) null,
    dosing_information tinyint(1) null,
    prescribing_markdown varchar(2000) null,
    source varchar(100) null,
    text_markdown varchar(4000) null,
    summary_markdown varchar(1000) null,
    raw text null,
    drug_id varchar(100) null,
    constraint drug_label_id_uindex
        unique (id)
);

alter table drug_label
    add primary key (id);

create table sample
(
    id int auto_increment
        primary key,
    created_at       datetime     null,
    uploaded_by      text         null,
    input_type       varchar(50)  null,
    file_name        text         null,
    parse_status     varchar(50)  null,
    matching_status  varchar(20)  null default 'pending',
    matched_at       datetime     null
);

create table variants2genotype
(
    id               int auto_increment primary key,
    gene_symbol      varchar(20)  not null,
    rsid             varchar(20)  not null,
    chromosome       varchar(10)  null,
    position         bigint       null,
    ref_allele       varchar(50)  null,
    alt_allele       varchar(50)  null,
    star_allele      varchar(100) not null,
    allele_function  varchar(100) null,
    is_required      tinyint(1)   null
);

create table genotype2phenotype
(
    id                int auto_increment primary key,
    gene_symbol       varchar(20)  not null,
    diplotype         varchar(100) not null,
    phenotype         varchar(100) not null,
    activity_score    varchar(20)  null,
    function_category varchar(100) null
);

create table matching_result
(
    id int auto_increment primary key,
    sample_id int not null,
    gene varchar(20) null,
    diplotype varchar(100) null,
    phenotype varchar(100) null,
    match_type varchar(20) null,
    drug_name varchar(200) null,
    source varchar(100) null,
    summary_markdown text null
);

