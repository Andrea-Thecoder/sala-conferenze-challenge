-- apply changes
create table app_refresh_token (
  id                            uuid not null,
  user_app_id                   uuid not null,
  family_id                     uuid not null,
  expires_at                    timestamp not null,
  revoked_at                    timestamp,
  replaced_by_id                uuid,
  version                       bigint not null,
  token_hash                    varchar(64) not null,
  constraint uq_app_refresh_token_token_hash unique (token_hash),
  constraint pk_app_refresh_token primary key (id)
);

create table a_booking (
  id                            uuid not null,
  conference_hall_id            uuid not null,
  user_app_id                   uuid not null,
  start_date_time               timestamp not null,
  end_date_time                 timestamp not null,
  total_cost                    NUMERIC(12,2) not null,
  paid                          boolean default false not null,
  version                       bigint not null,
  created_at                    timestamptz not null,
  updated_at                    timestamptz not null,
  created_by                    varchar(255) not null,
  updated_by                    varchar(255) not null,
  constraint pk_a_booking primary key (id)
);

create table building (
  id                            uuid not null,
  version                       bigint not null,
  created_at                    timestamptz not null,
  updated_at                    timestamptz not null,
  street                        varchar(255) not null,
  city                          varchar(100) not null,
  postal_code                   varchar(5) not null,
  country                       varchar(100) not null,
  created_by                    varchar(255) not null,
  updated_by                    varchar(255) not null,
  constraint pk_building primary key (id)
);

create table conference_hall (
  id                            uuid not null,
  size                          INTEGER DEFAULT 2 not null,
  price_per_hour                NUMERIC(12,2) not null,
  building_id                   uuid not null,
  floor                         integer not null,
  enabled                       boolean default false not null,
  version                       bigint not null,
  created_at                    timestamptz not null,
  updated_at                    timestamptz not null,
  name                          varchar(255) not null,
  note                          text,
  room_number                   varchar(20) not null,
  created_by                    varchar(255) not null,
  updated_by                    varchar(255) not null,
  constraint pk_conference_hall primary key (id)
);

create table app_user (
  id                            uuid not null,
  active                        boolean default false not null,
  version                       bigint not null,
  created_at                    timestamptz not null,
  updated_at                    timestamptz not null,
  first_name                    varchar(100) not null,
  last_name                     varchar(100) not null,
  password                      varchar(255) not null,
  email                         varchar(255) not null,
  phone_number                  varchar(16) not null,
  role                          varchar(9) not null,
  created_by                    varchar(255) not null,
  updated_by                    varchar(255) not null,
  constraint ck_app_user_role check ( role in ('ADMIN','ORGANIZER','CUSTOMER','REVOKED')),
  constraint uq_app_user_email unique (email),
  constraint uq_app_user_phone_number unique (phone_number),
  constraint pk_app_user primary key (id)
);

-- apply alter tables
alter table a_booking add column if not exists sys_period tstzrange not null default tstzrange(current_timestamp, null);
alter table app_refresh_token add column if not exists sys_period tstzrange not null default tstzrange(current_timestamp, null);
alter table building add column if not exists sys_period tstzrange not null default tstzrange(current_timestamp, null);
alter table conference_hall add column if not exists sys_period tstzrange not null default tstzrange(current_timestamp, null);
-- apply post alter
create table app_refresh_token_history(like app_refresh_token);
create view app_refresh_token_with_history as select * from app_refresh_token union all select * from app_refresh_token_history;
create or replace function app_refresh_token_history_version() returns trigger as $$
declare
  lowerTs timestamptz;
  upperTs timestamptz;
begin
  lowerTs = lower(OLD.sys_period);
  upperTs = greatest(lowerTs + '1 microsecond',current_timestamp);
  if (TG_OP = 'UPDATE') then
    insert into app_refresh_token_history (sys_period,id, user_app_id, token_hash, family_id, expires_at, revoked_at, replaced_by_id, version) values (tstzrange(lowerTs,upperTs), OLD.id, OLD.user_app_id, OLD.token_hash, OLD.family_id, OLD.expires_at, OLD.revoked_at, OLD.replaced_by_id, OLD.version);
    NEW.sys_period = tstzrange(upperTs,null);
    return new;
  elsif (TG_OP = 'DELETE') then
    insert into app_refresh_token_history (sys_period,id, user_app_id, token_hash, family_id, expires_at, revoked_at, replaced_by_id, version) values (tstzrange(lowerTs,upperTs), OLD.id, OLD.user_app_id, OLD.token_hash, OLD.family_id, OLD.expires_at, OLD.revoked_at, OLD.replaced_by_id, OLD.version);
    return old;
  end if;
end;
$$ LANGUAGE plpgsql;

create trigger app_refresh_token_history_upd
  before update or delete on app_refresh_token
  for each row execute procedure app_refresh_token_history_version();


update a_booking set sys_period = tstzrange(created_at, null);
create table a_booking_history(like a_booking);
create view a_booking_with_history as select * from a_booking union all select * from a_booking_history;
create or replace function a_booking_history_version() returns trigger as $$
declare
  lowerTs timestamptz;
  upperTs timestamptz;
begin
  lowerTs = lower(OLD.sys_period);
  upperTs = greatest(lowerTs + '1 microsecond',current_timestamp);
  if (TG_OP = 'UPDATE') then
    insert into a_booking_history (sys_period,id, conference_hall_id, user_app_id, start_date_time, end_date_time, total_cost, paid, version, created_at, updated_at, created_by, updated_by) values (tstzrange(lowerTs,upperTs), OLD.id, OLD.conference_hall_id, OLD.user_app_id, OLD.start_date_time, OLD.end_date_time, OLD.total_cost, OLD.paid, OLD.version, OLD.created_at, OLD.updated_at, OLD.created_by, OLD.updated_by);
    NEW.sys_period = tstzrange(upperTs,null);
    return new;
  elsif (TG_OP = 'DELETE') then
    insert into a_booking_history (sys_period,id, conference_hall_id, user_app_id, start_date_time, end_date_time, total_cost, paid, version, created_at, updated_at, created_by, updated_by) values (tstzrange(lowerTs,upperTs), OLD.id, OLD.conference_hall_id, OLD.user_app_id, OLD.start_date_time, OLD.end_date_time, OLD.total_cost, OLD.paid, OLD.version, OLD.created_at, OLD.updated_at, OLD.created_by, OLD.updated_by);
    return old;
  end if;
end;
$$ LANGUAGE plpgsql;

create trigger a_booking_history_upd
  before update or delete on a_booking
  for each row execute procedure a_booking_history_version();


update building set sys_period = tstzrange(created_at, null);
create table building_history(like building);
create view building_with_history as select * from building union all select * from building_history;
create or replace function building_history_version() returns trigger as $$
declare
  lowerTs timestamptz;
  upperTs timestamptz;
begin
  lowerTs = lower(OLD.sys_period);
  upperTs = greatest(lowerTs + '1 microsecond',current_timestamp);
  if (TG_OP = 'UPDATE') then
    insert into building_history (sys_period,id, street, city, postal_code, country, version, created_at, updated_at, created_by, updated_by) values (tstzrange(lowerTs,upperTs), OLD.id, OLD.street, OLD.city, OLD.postal_code, OLD.country, OLD.version, OLD.created_at, OLD.updated_at, OLD.created_by, OLD.updated_by);
    NEW.sys_period = tstzrange(upperTs,null);
    return new;
  elsif (TG_OP = 'DELETE') then
    insert into building_history (sys_period,id, street, city, postal_code, country, version, created_at, updated_at, created_by, updated_by) values (tstzrange(lowerTs,upperTs), OLD.id, OLD.street, OLD.city, OLD.postal_code, OLD.country, OLD.version, OLD.created_at, OLD.updated_at, OLD.created_by, OLD.updated_by);
    return old;
  end if;
end;
$$ LANGUAGE plpgsql;

create trigger building_history_upd
  before update or delete on building
  for each row execute procedure building_history_version();


update conference_hall set sys_period = tstzrange(created_at, null);
create table conference_hall_history(like conference_hall);
create view conference_hall_with_history as select * from conference_hall union all select * from conference_hall_history;
create or replace function conference_hall_history_version() returns trigger as $$
declare
  lowerTs timestamptz;
  upperTs timestamptz;
begin
  lowerTs = lower(OLD.sys_period);
  upperTs = greatest(lowerTs + '1 microsecond',current_timestamp);
  if (TG_OP = 'UPDATE') then
    insert into conference_hall_history (sys_period,id, name, note, size, price_per_hour, building_id, floor, room_number, enabled, version, created_at, updated_at, created_by, updated_by) values (tstzrange(lowerTs,upperTs), OLD.id, OLD.name, OLD.note, OLD.size, OLD.price_per_hour, OLD.building_id, OLD.floor, OLD.room_number, OLD.enabled, OLD.version, OLD.created_at, OLD.updated_at, OLD.created_by, OLD.updated_by);
    NEW.sys_period = tstzrange(upperTs,null);
    return new;
  elsif (TG_OP = 'DELETE') then
    insert into conference_hall_history (sys_period,id, name, note, size, price_per_hour, building_id, floor, room_number, enabled, version, created_at, updated_at, created_by, updated_by) values (tstzrange(lowerTs,upperTs), OLD.id, OLD.name, OLD.note, OLD.size, OLD.price_per_hour, OLD.building_id, OLD.floor, OLD.room_number, OLD.enabled, OLD.version, OLD.created_at, OLD.updated_at, OLD.created_by, OLD.updated_by);
    return old;
  end if;
end;
$$ LANGUAGE plpgsql;

create trigger conference_hall_history_upd
  before update or delete on conference_hall
  for each row execute procedure conference_hall_history_version();


-- foreign keys and indices
create index ix_app_refresh_token_user_app_id on app_refresh_token (user_app_id);
alter table app_refresh_token add constraint fk_app_refresh_token_user_app_id foreign key (user_app_id) references app_user (id) on delete restrict on update restrict;

create index ix_app_refresh_token_replaced_by_id on app_refresh_token (replaced_by_id);
alter table app_refresh_token add constraint fk_app_refresh_token_replaced_by_id foreign key (replaced_by_id) references app_refresh_token (id) on delete restrict on update restrict;

create index ix_a_booking_conference_hall_id on a_booking (conference_hall_id);
alter table a_booking add constraint fk_a_booking_conference_hall_id foreign key (conference_hall_id) references conference_hall (id) on delete restrict on update restrict;

create index ix_a_booking_user_app_id on a_booking (user_app_id);
alter table a_booking add constraint fk_a_booking_user_app_id foreign key (user_app_id) references app_user (id) on delete restrict on update restrict;

create index ix_conference_hall_building_id on conference_hall (building_id);
alter table conference_hall add constraint fk_conference_hall_building_id foreign key (building_id) references building (id) on delete restrict on update restrict;

create index if not exists ix_app_refresh_token_family_id on app_refresh_token (family_id);
create index if not exists idx_booking_hall_time_range on a_booking (conference_hall_id,start_date_time,end_date_time);
create index if not exists ix_a_booking_start_date_time on a_booking (start_date_time);
create index if not exists ix_a_booking_end_date_time on a_booking (end_date_time);
create index if not exists ix_building_street on building (street);
create index if not exists ix_building_city on building (city);
create index if not exists ix_building_postal_code on building (postal_code);
create index if not exists ix_building_country on building (country);
create index if not exists ix_conference_hall_name on conference_hall (name);
create index if not exists ix_conference_hall_size on conference_hall (size);
create index if not exists ix_conference_hall_price_per_hour on conference_hall (price_per_hour);
create index if not exists ix_conference_hall_enabled on conference_hall (enabled);
create index if not exists idx_app_user_firstname_lastname on app_user (first_name,last_name);
create index if not exists ix_app_user_last_name on app_user (last_name);
create index if not exists ix_app_user_role on app_user (role);
create index if not exists ix_app_user_active on app_user (active);
