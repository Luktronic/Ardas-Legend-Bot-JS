create table if not exists armies (id int8 generated by default as identity, army_type varchar(255) not null, created_at timestamp, free_tokens float8 not null, heal_end timestamp, heal_start timestamp, hours_healed int4, hours_left_healing int4, is_healing boolean, is_paid boolean, name varchar(255), current_region varchar(255) not null, faction int8 not null, origin_claimbuild int8, stationed_at int8, primary key (id));
create table if not exists army_sieges (army_id int8 not null, sieges varchar(255));
create table if not exists battle_attacking_armies (battle_id int8 not null, atacking_army_id int8 not null, primary key (battle_id, atacking_army_id));
create table if not exists battle_defending_armies (battle_id int8 not null, defending_army_id int8 not null, primary key (battle_id, defending_army_id));
create table if not exists battles (id int8 generated by default as identity, agreed_battle_date timestamp, field_battle boolean, declared_date timestamp, name varchar(255), time_frozen_from timestamp, time_frozen_until timestamp, claimbuild_id int8, region_id varchar(255), war_id int8, primary key (id));
create table if not exists claimbuild_application_production_sites (claimbuild_id int8 not null, count int8, production_site_id int8);
create table if not exists claimbuild_apps (id int8 generated by default as identity, version int4, applied_at timestamp not null, discord_accepted_message_link varchar(255), discord_application_message_link varchar(255) not null, last_vote_at timestamp not null, resolved_at timestamp, state varchar(255) not null, vote_count int2 not null, claim_build_type varchar(255) not null, claimbuild_name varchar(255), x int4, y int4, z int4, number_of_houses varchar(255), siege varchar(255), traders varchar(255), player_id int8 not null, owned_by_id int8 not null, region_id varchar(255) not null, primary key (id));
create table if not exists claimbuild_apps_accepted_by (claimbuild_application_id int8 not null, accepted_by_id int8 not null, primary key (claimbuild_application_id, accepted_by_id));
create table if not exists claimbuild_apps_built_by (claimbuild_application_id int8 not null, built_by_id int8 not null, primary key (claimbuild_application_id, built_by_id));
create table if not exists claimbuild_builders (claimbuild_id int8 not null, player_id int8 not null, primary key (claimbuild_id, player_id));
create table if not exists claimbuild_special_buildings (claimbuild_id int8 not null, special_buildings varchar(255));
create table if not exists claimbuild_application_special_buildings (claimbuild_application_id int8 not null, special_buildings varchar(255));
create table if not exists claimbuilds (id int8 generated by default as identity, version int4, x int4, y int4, z int4, free_armies_remaining int4 not null, free_trading_companies_remaining int4 not null, name varchar(255), number_of_houses varchar(255), siege varchar(255), traders varchar(255), type varchar(255) not null, owned_by int8 not null, region varchar(255) not null, primary key (id));
create table if not exists faction_aliases (faction_id int8 not null, aliases varchar(255));
create table if not exists faction_allies (faction int8 not null, ally_faction int8 not null);
create table if not exists faction_claimed_regions (region varchar(255) not null, faction int8 not null, primary key (region, faction));
create table if not exists factions (id int8 generated by default as identity, colorcode varchar(255), faction_buff_descr varchar(512), role_id int8, food_stockpile int4, initial_faction int4, name varchar(255), home_region_id varchar(255), leader_id int8, primary key (id));
create table if not exists movement_path (movement_id int8 not null, actual_cost int4, base_cost int4, region_id varchar(255));
create table if not exists movements (id int8 generated by default as identity, end_time timestamp, hours_moved int4, hours_until_complete int4, hours_until_next_region int4, is_char_movement boolean, is_currently_active boolean, start_time timestamp, army_name int8, rpchar_id int8, primary key (id));
create table if not exists players (id int8 generated by default as identity, discord_id varchar(255) not null, ign varchar(255) not null, is_staff boolean, uuid varchar(255) not null, faction int8 not null, primary key (id));
create table if not exists production_claimbuild (claimbuild_id int8 not null, production_site_id int8 not null, count int8, primary key (claimbuild_id, production_site_id));
create table if not exists production_sites (id int8 generated by default as identity, amount_produced int4, type varchar(255), produced_resource varchar(255), primary key (id));
create table if not exists region_neighbours (region varchar(255) not null, neighbour varchar(255) not null, primary key (region, neighbour));
create table if not exists regions (id varchar(255) not null, has_ownership_changed_since_last_claimmap_update boolean, name varchar(255), region_type varchar(255), primary key (id));
create table if not exists resources (id int8 generated by default as identity, minecraft_item_id varchar(255), resource_name varchar(255), resource_type varchar(255), primary key (id));
create table if not exists roleplay_apps (id int8 generated by default as identity, version int4, applied_at timestamp not null, discord_accepted_message_link varchar(255), discord_application_message_link varchar(255) not null, last_vote_at timestamp not null, resolved_at timestamp, state varchar(255) not null, vote_count int2 not null, character_name varchar(255), character_title varchar(255), gear varchar(255), link_to_lore varchar(255), pvp boolean not null, why_do_you_want_to_be_this_character varchar(255), player_id int8 not null, faction_id int8 not null, primary key (id));
create table if not exists roleplay_apps_accepted_by (roleplay_application_id int8 not null, accepted_by_id int8 not null, primary key (roleplay_application_id, accepted_by_id));
create table if not exists rpchars (id int8 generated by default as identity, version int4, active boolean, gear varchar(255), heal_ends timestamp, injured boolean, is_healing boolean, link_to_lore varchar(255), name varchar(255), pvp boolean, started_heal timestamp, title varchar(25), bound_to int8, current_region varchar(255) not null, owner_id int8, primary key (id));
create table if not exists unit_types (unit_name varchar(255) not null, token_cost float8 not null, primary key (unit_name));
create table if not exists units (id int8 generated by default as identity, amount_alive int4, count int4, is_mounted boolean, army int8, unit_type varchar(255), primary key (id));
create table if not exists war_aggressors (war_id int8 not null, initial_party boolean, joining_date timestamp, participant_faction_id int8);
create table if not exists war_defenders (war_id int8 not null, initial_party boolean, joining_date timestamp, participant_faction_id int8);
create table if not exists wars (id int8 generated by default as identity, end_date timestamp, name varchar(255) not null, start_date timestamp not null, primary key (id));
alter table if exists armies add constraint UK_t4y43fiehdq8u2vbhmvx2wkr6 unique (name);
alter table if exists claimbuild_apps_accepted_by add constraint UK_5k0wchqlaw8d2epkwir9oq9mv unique (accepted_by_id);
alter table if exists claimbuilds add constraint UK_pak8lmje62n4ma2jbkcn0fh9c unique (name);
alter table if exists factions add constraint UK_47tn5065hoyr2hbbodcfo5rpe unique (role_id);
alter table if exists factions add constraint UK_e5v4rshlkexwkyr9ttmk2ksxl unique (name);
alter table if exists players add constraint UK_n8wow9trjj08swysdxpd71a2l unique (discord_id);
alter table if exists players add constraint UK_qtxh40udrkc84tcpyk1iqpaee unique (ign);
alter table if exists players add constraint UK_59owtm6p9ubo516h8sqiefm18 unique (uuid);
alter table if exists resources add constraint UK_7vh3yrsm7oyl3lljngbasr6qj unique (resource_name);
alter table if exists roleplay_apps_accepted_by add constraint UK_27e1hgu7k3qyebo1d1wtfwjkn unique (accepted_by_id);
alter table if exists rpchars add constraint UK_c5945ixwr1xfvnhgl4xmi0cpu unique (name);