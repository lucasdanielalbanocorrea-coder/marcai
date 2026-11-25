-- Ids de mensagens do webhook já tratadas: a Meta reenvia eventos, então cada um é processado uma única vez.
CREATE TABLE evento_processado (
    id            VARCHAR(255) PRIMARY KEY,
    processado_em TIMESTAMPTZ  NOT NULL DEFAULT now()
);
