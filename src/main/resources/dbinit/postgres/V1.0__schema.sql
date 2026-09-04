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
  created_at                    timestamp not null,
  updated_at                    timestamp not null,
  created_by                    varchar(255) not null,
  updated_by                    varchar(255) not null,
  constraint pk_a_booking primary key (id)
);

create table building (
  id                            uuid not null,
  version                       bigint not null,
  created_at                    timestamp not null,
  updated_at                    timestamp not null,
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
  created_at                    timestamp not null,
  updated_at                    timestamp not null,
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
  created_at                    timestamp not null,
  updated_at                    timestamp not null,
  first_name                    varchar(100) not null,
  last_name                     varchar(100) not null,
  password                      varchar(255) not null,
  email                         varchar(255) not null,
  phone_number                  varchar(20) not null,
  role                          varchar(9) not null,
  created_by                    varchar(255) not null,
  updated_by                    varchar(255) not null,
  constraint ck_app_user_role check ( role in ('ADMIN','ORGANIZER','CUSTOMER','REVOKED')),
  constraint uq_app_user_email unique (email),
  constraint pk_app_user primary key (id)
);

-- foreign keys and indices
alter table app_refresh_token add constraint fk_app_refresh_token_user_app_id foreign key (user_app_id) references app_user (id) on delete restrict on update restrict;

create index ix_app_refresh_token_replaced_by_id on app_refresh_token (replaced_by_id);
alter table app_refresh_token add constraint fk_app_refresh_token_replaced_by_id foreign key (replaced_by_id) references app_refresh_token (id) on delete restrict on update restrict;

create index ix_a_booking_conference_hall_id on a_booking (conference_hall_id);
alter table a_booking add constraint fk_a_booking_conference_hall_id foreign key (conference_hall_id) references conference_hall (id) on delete restrict on update restrict;

create index ix_a_booking_user_app_id on a_booking (user_app_id);
alter table a_booking add constraint fk_a_booking_user_app_id foreign key (user_app_id) references app_user (id) on delete restrict on update restrict;

create index ix_conference_hall_building_id on conference_hall (building_id);
alter table conference_hall add constraint fk_conference_hall_building_id foreign key (building_id) references building (id) on delete restrict on update restrict;

create index if not exists idx_refresh_token_family on app_refresh_token (family_id);
create index if not exists idx_refresh_token_user on app_refresh_token (user_app_id);
create index if not exists idx_booking_hall_time_range on a_booking (conference_hall_id,start_date_time,end_date_time);
create index if not exists idx_app_user_phone_number on app_user (phone_number);
create index if not exists idx_app_user_email on app_user (email);
create index if not exists idx_app_user_lastname on app_user (last_name);
create index if not exists idx_app_user_firstname_lastname on app_user (first_name,last_name);
