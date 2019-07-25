
FROM "debezium/postgres:11-alpine"

COPY init.sql /docker-entrypoint-initdb.d/zzz-init_tables.sql
COPY insert_data.sh ./insert_data.sh
COPY entrypoint.sh ./entrypoint.sh

RUN ["chmod" , "+x", "entrypoint.sh"]
#RUN ["sed", "$ s/.*/]/g", "docker-entrypoint.sh"]
#RUN ["echo", "exec insert_data.sh", ">>", "docker-entrypoint.sh"]

USER postgres
ENTRYPOINT ["/entrypoint.sh"]
EXPOSE 5432
CMD ["postgres"]
