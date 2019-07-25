CREATE DATABASE intueri_demo;

\connect intueri_demo;

CREATE TABLE demo
(
  id         serial primary key,
  varcharCol varchar(40),
  integerCol integer,
  dateCol    date,
  doubleCol  double precision
);
