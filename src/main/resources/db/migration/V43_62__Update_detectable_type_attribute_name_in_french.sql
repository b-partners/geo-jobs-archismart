alter type detectable_type rename value 'ROOF' to 'TOITURE_REVETEMENT';
alter type detectable_type rename value 'SOLAR_PANEL' to 'PANNEAU_PHOTOVOLTAIQUE';
alter type detectable_type rename value 'POOL' to 'PISCINE';
alter type detectable_type rename value 'PATHWAY' to 'PASSAGE_PIETON';
alter type detectable_type rename value 'TREE' to 'ARBRE';

alter type detectable_type add value if not exists 'ESPACE_VERT';
alter type detectable_type add value if not exists 'TROTTOIR';