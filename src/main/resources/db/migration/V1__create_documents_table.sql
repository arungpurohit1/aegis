CREATE TABLE documents (
    id          uuid                     NOT NULL,
    title       varchar(200)             NOT NULL,
    content     text,
    created_at  timestamp(6) with time zone NOT NULL,
    updated_at  timestamp(6) with time zone NOT NULL,
    PRIMARY KEY (id)
);
