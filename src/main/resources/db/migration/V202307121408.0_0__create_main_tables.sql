CREATE TABLE ORDERS_PRICE (
    ID INT NOT NULL,
    ACCRUAL_POINTS NUMBER(10,2),
    AMOUNT NUMBER(10,2),
    POINTS_AMOUNT NUMBER(10,2),
    PARTNER_AMOUNT NUMBER(10,2),
    PRICE_LIST_ID VARCHAR2(255)  NOT NULL,
    PRICE_LIST_DESCRIPTION VARCHAR2(255) NULL,
    CREATE_DATE TIMESTAMP WITH TIME ZONE,
    LAST_MODIFIED_DATE TIMESTAMP WITH TIME ZONE
);
CREATE SEQUENCE ORDERS_PRICE_SEQ START WITH 1;
ALTER TABLE ORDERS_PRICE ADD CONSTRAINT PK_ORDERS_PRICE_ID PRIMARY KEY (ID) ENABLE;
COMMENT ON COLUMN ORDERS_PRICE.ACCRUAL_POINTS is 'acumulo de pontos';
COMMENT ON COLUMN ORDERS_PRICE.AMOUNT is 'valor livelo';
COMMENT ON COLUMN ORDERS_PRICE.POINTS_AMOUNT is 'valor em pontos';
COMMENT ON COLUMN ORDERS_PRICE.PARTNER_AMOUNT is 'valor parceiro';
COMMENT ON COLUMN ORDERS_PRICE.PRICE_LIST_ID is 'id da lista de preço livelo clube / não clube';
COMMENT ON COLUMN ORDERS_PRICE.PRICE_LIST_DESCRIPTION is 'descrição da lista de preços livelo';

CREATE TABLE ORDERS (
    ID VARCHAR2(255) NOT NULL,
    COMMERCE_ORDER_ID VARCHAR2(255) NOT NULL,
    PARTNER_ORDER_ID VARCHAR2(500) NULL,
    PARTNER_CODE VARCHAR2(255),
    SUBMITTED_DATE TIMESTAMP WITH TIME ZONE,
    CHANNEL VARCHAR2(100) NULL,
    TIER_CODE VARCHAR2(255) NULL,
    ORIGIN_ORDER VARCHAR2(255) NULL,
    CUSTOMER_IDENTIFIER VARCHAR2(255) NULL,
    TRANSACTION_ID VARCHAR2(255) NULL,
    EXPIRATION_DATE TIMESTAMP WITH TIME ZONE,
    STATUS INT,
    ORDER_PRICE_ID INT NULL,
    CREATE_DATE TIMESTAMP WITH TIME ZONE,
    LAST_MODIFIED_DATE TIMESTAMP WITH TIME ZONE
);
CREATE SEQUENCE ORDERS_SEQ START WITH 1;
CREATE INDEX ORDERS_PRICE_ID_INDEX on ORDERS(ORDER_PRICE_ID) online;
CREATE INDEX ORDERS_CUSTOMER_INDEX on ORDERS(CUSTOMER_IDENTIFIER) online;
CREATE INDEX ORDERS_COMMERCE_ORDER_ID_INDEX on ORDERS(COMMERCE_ORDER_ID) online;
CREATE INDEX ORDERS_PARTNER_ORDER_ID_INDEX on ORDERS(PARTNER_ORDER_ID) online;
CREATE INDEX ORDERS_PARTNER_CODE_INDEX on ORDERS(PARTNER_CODE) online;
ALTER TABLE ORDERS ADD CONSTRAINT PK_ORDERS_ID PRIMARY KEY (ID) ENABLE;
ALTER TABLE ORDERS ADD CONSTRAINT fk_ORDERS_order_price_id FOREIGN KEY (ORDER_PRICE_ID) REFERENCES ORDERS_PRICE (ID);
COMMENT ON COLUMN ORDERS.COMMERCE_ORDER_ID is 'id gerado pelo carrinho livelo';
COMMENT ON COLUMN ORDERS.PARTNER_ORDER_ID is 'id da reserva no parceiro';
COMMENT ON COLUMN ORDERS.PARTNER_CODE is 'código do parceiro (cvc)';
COMMENT ON COLUMN ORDERS.SUBMITTED_DATE is 'data da finalização do pedido';
COMMENT ON COLUMN ORDERS.CHANNEL is 'canal de compra do participante (desktop/app)';
COMMENT ON COLUMN ORDERS.TIER_CODE is 'categoria do participante (clube(nível)/não clube) **confirmar';
COMMENT ON COLUMN ORDERS.ORIGIN_ORDER is 'se veio de algum canal externo (zoom, buscapé) **confirmar';
COMMENT ON COLUMN ORDERS.CUSTOMER_IDENTIFIER is 'id do participante';
COMMENT ON COLUMN ORDERS.TRANSACTION_ID is 'id correlacinal';
COMMENT ON COLUMN ORDERS.EXPIRATION_DATE is 'data de expiração do booking/reserva';

CREATE TABLE ORDERS_STATUS(
    ID INT NOT NULL,
    CODE VARCHAR2(50) NOT NULL,
    DESCRIPTION VARCHAR2(255) NOT NULL,
    PARTNER_CODE VARCHAR2(255),
    PARTNER_DESCRIPTION VARCHAR2(255),
    PARTNER_RESPONSE CLOB,
    STATUS_DATE TIMESTAMP WITH TIME ZONE,
    ORDER_ID VARCHAR2(255) NULL,
    CREATE_DATE TIMESTAMP WITH TIME ZONE,
    LAST_MODIFIED_DATE TIMESTAMP WITH TIME ZONE
);
CREATE SEQUENCE ORDERS_STATUS_SEQ START WITH 1;
CREATE INDEX ORDERS_STATUS_ORDER_ID_INDEX on ORDERS_STATUS(ORDER_ID) online;
CREATE INDEX ORDERS_STATUS_CODE_INDEX on ORDERS_STATUS(CODE) online;
ALTER TABLE ORDERS_STATUS ADD CONSTRAINT PK_ORDERS_STATUS_ID PRIMARY KEY (ID) ENABLE;
ALTER TABLE ORDERS_STATUS ADD CONSTRAINT fk_orders_status_order_id FOREIGN KEY (ORDER_ID) REFERENCES ORDERS (ID);
ALTER TABLE ORDERS ADD CONSTRAINT fk_orders_status_id FOREIGN KEY (STATUS) REFERENCES ORDERS_STATUS (ID);
CREATE INDEX ORDERS_STATUS_INDEX on ORDERS(STATUS) online;
COMMENT ON COLUMN ORDERS_STATUS.CODE is 'código status livelo';
COMMENT ON COLUMN ORDERS_STATUS.DESCRIPTION is 'descrição status livelo';
COMMENT ON COLUMN ORDERS_STATUS.PARTNER_CODE is 'código status parceiro';
COMMENT ON COLUMN ORDERS_STATUS.PARTNER_DESCRIPTION is 'descrição status parceiro';
COMMENT ON COLUMN ORDERS_STATUS.PARTNER_RESPONSE is 'body de retorno do parceiro';
COMMENT ON COLUMN ORDERS_STATUS.STATUS_DATE is 'data de atualização do status';

CREATE TABLE ORDERS_PRICE_DESCRIPTION (
    ID INT NOT NULL,
    AMOUNT NUMBER(10,2),
    POINTS_AMOUNT NUMBER(10,2),
    TYPE VARCHAR2(255) NOT NULL,
    DESCRIPTION VARCHAR2(255) NULL,
    ORDER_PRICE_ID INT,
    CREATE_DATE TIMESTAMP WITH TIME ZONE,
    LAST_MODIFIED_DATE TIMESTAMP WITH TIME ZONE
);
CREATE SEQUENCE ORDERS_PRICE_DESCRIPTION_SEQ START WITH 1;
CREATE INDEX ORDERS_PRICE_DESCRIPTION_ORDER_PRICE_ID_INDEX on ORDERS_PRICE_DESCRIPTION(ORDER_PRICE_ID) online;
ALTER TABLE ORDERS_PRICE_DESCRIPTION ADD CONSTRAINT PK_ORDERS_PRICE_DESCRIPTION_ID PRIMARY KEY (ID) ENABLE;
ALTER TABLE ORDERS_PRICE_DESCRIPTION ADD CONSTRAINT fk_orders_price_desc_order_price_id FOREIGN KEY (ORDER_PRICE_ID) REFERENCES ORDERS_PRICE (ID);
COMMENT ON COLUMN ORDERS_PRICE_DESCRIPTION.AMOUNT is 'valor livelo';
COMMENT ON COLUMN ORDERS_PRICE_DESCRIPTION.POINTS_AMOUNT is 'valor pontos livelo';
COMMENT ON COLUMN ORDERS_PRICE_DESCRIPTION.TYPE is 'tipo da descrição do preço (ADULT, CHILD, TAX)';
COMMENT ON COLUMN ORDERS_PRICE_DESCRIPTION.DESCRIPTION is 'descrição do tipo da taxa a ser exibido no front';

CREATE TABLE TRAVEL_INFO (
    ID INT NOT NULL,
    TYPE VARCHAR2(255) NOT NULL,
    RESERVATION_CODE VARCHAR2(255),
    ADULT_QUANTITY INT,
    CHILD_QUANTITY INT,
    BABY_QUANTITY INT,
    VOUCHER VARCHAR2(255),
    TYPE_CLASS VARCHAR2(255),
    CREATE_DATE TIMESTAMP WITH TIME ZONE,
    LAST_MODIFIED_DATE TIMESTAMP WITH TIME ZONE
);
CREATE SEQUENCE TRAVEL_INFO_SEQ START WITH 1;
ALTER TABLE TRAVEL_INFO ADD CONSTRAINT PK_TRAVEL_INFO_ID PRIMARY KEY (ID) ENABLE;
COMMENT ON COLUMN TRAVEL_INFO.TYPE is 'tipo do trecho (ida, ida/volta, multitrecho)';
COMMENT ON COLUMN TRAVEL_INFO.RESERVATION_CODE is 'codigo da reserva na companhia aérea';
COMMENT ON COLUMN TRAVEL_INFO.ADULT_QUANTITY is 'quantidade adultos';
COMMENT ON COLUMN TRAVEL_INFO.CHILD_QUANTITY is 'quantidade crianças';
COMMENT ON COLUMN TRAVEL_INFO.BABY_QUANTITY is 'quantidade bebes';
COMMENT ON COLUMN TRAVEL_INFO.VOUCHER is 'ticket da passagem';
COMMENT ON COLUMN TRAVEL_INFO.TYPE_CLASS is 'classe escolhida pelo cliente (econômica/executiva)';

CREATE TABLE ORDERS_ITEM_PRICE (
    ID INT NOT NULL,
    LIST_PRICE VARCHAR2(255),
    AMOUNT NUMBER(15,4),
    POINTS_AMOUNT NUMBER(10,2),
    ACCRUAL_POINTS NUMBER(10,2),
    PARTNER_AMOUNT NUMBER(10,2),
    PRICE_LIST_ID VARCHAR2(255) NOT NULL,
    PRICE_RULE CLOB NULL,
    CREATE_DATE TIMESTAMP WITH TIME ZONE,
    LAST_MODIFIED_DATE TIMESTAMP WITH TIME ZONE
);
CREATE SEQUENCE ORDERS_ITEM_PRICE_SEQ START WITH 1;
ALTER TABLE ORDERS_ITEM_PRICE ADD CONSTRAINT PK_ORDERS_ITEM_PRICE_ID PRIMARY KEY (ID) ENABLE;
COMMENT ON COLUMN ORDERS_ITEM_PRICE.LIST_PRICE is 'preço DE - POR';
COMMENT ON COLUMN ORDERS_ITEM_PRICE.AMOUNT is 'valor item livelo';
COMMENT ON COLUMN ORDERS_ITEM_PRICE.POINTS_AMOUNT is 'valor item pontos livelo';
COMMENT ON COLUMN ORDERS_ITEM_PRICE.ACCRUAL_POINTS is 'valor acumulo item';
COMMENT ON COLUMN ORDERS_ITEM_PRICE.PARTNER_AMOUNT is 'valor item parceiro';
COMMENT ON COLUMN ORDERS_ITEM_PRICE.PRICE_LIST_ID is 'lista de preços (clube/não clube)';
COMMENT ON COLUMN ORDERS_ITEM_PRICE.PRICE_RULE is 'regra de preço do parceiro completa armazenada na calculadora';

CREATE TABLE ORDERS_ITEM (
    ID INT NOT NULL,
    COMMERCE_ITEM_ID VARCHAR2(255) NOT NULL,
    SKU_ID VARCHAR2(255) NOT NULL,
    PRODUCT_ID VARCHAR2(255) NULL,
    QUANTITY INT,
    EXTERNAL_COUPON VARCHAR2(255),
    ORDER_ID VARCHAR2(255) NULL,
    TRAVEL_INFO_ID INT NULL,
    ORDER_ITEM_PRICE_ID INT NULL,
    CREATE_DATE TIMESTAMP WITH TIME ZONE,
    LAST_MODIFIED_DATE TIMESTAMP WITH TIME ZONE
);
CREATE SEQUENCE ORDERS_ITEM_SEQ START WITH 1;
CREATE INDEX ORDERS_ITEM_ORDER_ID_INDEX on ORDERS_ITEM(ORDER_ID) online;
CREATE INDEX ORDERS_ITEM_TRAVEL_INFO_ID_INDEX on ORDERS_ITEM(TRAVEL_INFO_ID) online;
CREATE INDEX ORDERS_ITEM_COMMERCE_ITEM_ID_INDEX on ORDERS_ITEM(COMMERCE_ITEM_ID) online;
CREATE INDEX ORDERS_ITEM_ORDER_ITEM_PRICE_ID_INDEX on ORDERS_ITEM(ORDER_ITEM_PRICE_ID) online;
ALTER TABLE ORDERS_ITEM ADD CONSTRAINT PK_ORDERS_ITEM_ID PRIMARY KEY (ID) ENABLE;
ALTER TABLE ORDERS_ITEM ADD CONSTRAINT fk_orders_item_order_id FOREIGN KEY (ORDER_ID) REFERENCES ORDERS (ID);
ALTER TABLE ORDERS_ITEM ADD CONSTRAINT fk_orders_item_travel_info_id FOREIGN KEY (TRAVEL_INFO_ID) REFERENCES TRAVEL_INFO (ID);
ALTER TABLE ORDERS_ITEM ADD CONSTRAINT fk_orders_item_order_item_price_id FOREIGN KEY (ORDER_ITEM_PRICE_ID) REFERENCES ORDERS_ITEM_PRICE (ID);
COMMENT ON COLUMN ORDERS_ITEM.COMMERCE_ITEM_ID is 'id gerado pelo carrinho para o item';
COMMENT ON COLUMN ORDERS_ITEM.SKU_ID is 'sku do produto (cvc_flight/cvc_flight_tax)';
COMMENT ON COLUMN ORDERS_ITEM.PRODUCT_ID  is 'tipo do produto (flight)';
COMMENT ON COLUMN ORDERS_ITEM.QUANTITY is 'quantidade de itens';
COMMENT ON COLUMN ORDERS_ITEM.EXTERNAL_COUPON is 'cupom promocional';

CREATE TABLE PAX (
    ID INT NOT NULL,
    TYPE VARCHAR2(255) NOT NULL,
    FIRST_NAME VARCHAR2(255) NOT NULL,
    LAST_NAME VARCHAR2(255) NOT NULL,
    EMAIL VARCHAR2(255) NULL,
    AREA_CODE VARCHAR2(2) NULL,
    PHONE_NUMBER VARCHAR2(9) NULL,
    GENDER VARCHAR2(1) NULL,
    BIRTH_DATE VARCHAR2(255),
    TRAVEL_INFO_ID INT NULL,
    CREATE_DATE TIMESTAMP WITH TIME ZONE,
    LAST_MODIFIED_DATE TIMESTAMP WITH TIME ZONE
);
CREATE SEQUENCE PAX_SEQ START WITH 1;
CREATE INDEX PAX_TRAVEL_INFO_ID_INDEX on PAX(TRAVEL_INFO_ID) online;
ALTER TABLE PAX ADD CONSTRAINT PK_PAX_ID PRIMARY KEY (ID) ENABLE;
ALTER TABLE PAX ADD CONSTRAINT fk_pax_travel_info_id FOREIGN KEY (TRAVEL_INFO_ID) REFERENCES TRAVEL_INFO (ID);
COMMENT ON COLUMN PAX.TYPE is 'tipo passageiro (ADULT,CHILD,BABY)';
COMMENT ON COLUMN PAX.AREA_CODE is 'codigo de area do telefone';
COMMENT ON COLUMN PAX.PHONE_NUMBER is 'numero do telefone';
COMMENT ON COLUMN PAX.GENDER is 'genero passageiro';

CREATE TABLE DOCUMENT (
	ID INT NOT NULL,
	DOCUMENT_NUMBER VARCHAR2(255) NOT NULL,
	TYPE VARCHAR2(10) NOT NULL,
	ISSUE_DATE VARCHAR2(10) NOT NULL,
	ISSUING_COUNTRY VARCHAR2(100) NOT NULL,
    EXPIRATION_DATE VARCHAR2(10) NOT NULL,
	RESIDENCE_COUNTRY VARCHAR2(100) NOT NULL,
	PAX_ID INT NULL,
	CREATE_DATE TIMESTAMP WITH TIME ZONE,
	LAST_MODIFIED_DATE TIMESTAMP WITH TIME ZONE
);
CREATE SEQUENCE DOCUMENT_SEQ START WITH 1;
CREATE INDEX DOCUMENT_PAX_ID_INDEX on DOCUMENT(PAX_ID) online;
ALTER TABLE DOCUMENT ADD CONSTRAINT PK_DOCUMENT_ID PRIMARY KEY (ID) ENABLE;
ALTER TABLE DOCUMENT ADD CONSTRAINT fk_document_pax_id FOREIGN KEY (PAX_ID) REFERENCES PAX (ID);
COMMENT ON COLUMN DOCUMENT.DOCUMENT_NUMBER is 'numero documento';
COMMENT ON COLUMN DOCUMENT.TYPE is 'tipo documento (cpf/passaporte)';
COMMENT ON COLUMN DOCUMENT.ISSUE_DATE is 'data de emissão';
COMMENT ON COLUMN DOCUMENT.ISSUING_COUNTRY is 'pais de emissão';
COMMENT ON COLUMN DOCUMENT.EXPIRATION_DATE is 'data expiração';
COMMENT ON COLUMN DOCUMENT.RESIDENCE_COUNTRY is 'pais de residência';

--SEGMENT REFERE-SE AO TRECHO (PODE HAVER MAIS DE UM VÔO NO TRECHO)
CREATE TABLE SEGMENT (
    ID INT NOT NULL,
    PARTNER_ID VARCHAR2(255),
    STEP INT,
    STOPS INT,
    FLIGHT_DURATION INT,
    ORIGIN_IATA VARCHAR2(3) NOT NULL,
    ORIGIN_DESCRIPTION VARCHAR2(255) NULL,
    DESTINATION_IATA VARCHAR2(3) NOT NULL,
    DESTINATION_DESCRIPTION VARCHAR2(255) NULL,
    DEPARTURE_DATE TIMESTAMP WITH TIME ZONE,
    ARRIVAL_DATE TIMESTAMP WITH TIME ZONE,
    ORDER_ITEM_ID INT NULL,
    CREATE_DATE TIMESTAMP WITH TIME ZONE,
    LAST_MODIFIED_DATE TIMESTAMP WITH TIME ZONE
);
CREATE SEQUENCE SEGMENT_SEQ START WITH 1;
CREATE INDEX SEGMENT_ORDERS_ITEM_ID_INDEX on SEGMENT(ORDER_ITEM_ID) online;
ALTER TABLE SEGMENT ADD CONSTRAINT PK_SEGMENT_ID PRIMARY KEY (ID) ENABLE;
ALTER TABLE SEGMENT ADD CONSTRAINT fk_segment_orders_item_id FOREIGN KEY (ORDER_ITEM_ID) REFERENCES ORDERS_ITEM (ID);
COMMENT ON COLUMN SEGMENT.PARTNER_ID is 'IDENTIFICADOR DO VÔO NO PARCEIRO (TOKEN)';
COMMENT ON COLUMN SEGMENT.STEP is 'NÚMERO DO TRECHO DA VIAGEM';
COMMENT ON COLUMN SEGMENT.STOPS is 'NÚMERO DE PARADAS';
COMMENT ON COLUMN SEGMENT.FLIGHT_DURATION is 'DURAÇÃO DO VÔO EM MINUTOS';
COMMENT ON COLUMN SEGMENT.ORIGIN_IATA is 'IATA ORIGEM';
COMMENT ON COLUMN SEGMENT.ORIGIN_DESCRIPTION is 'IATA DESCRIÇÃO / ORIGEM';
COMMENT ON COLUMN SEGMENT.DESTINATION_IATA is 'IATA DESTINO';
COMMENT ON COLUMN SEGMENT.DESTINATION_DESCRIPTION is 'IATA DESCRIÇÃO DESTINO';
COMMENT ON COLUMN SEGMENT.DEPARTURE_DATE is 'DATA E HORA DA PARTIDA DO TRECHO';
COMMENT ON COLUMN SEGMENT.ARRIVAL_DATE is 'DATA E HORA DE CHEGADA DO TRECHO';

--DESCRIÇÃO DE UM VÔO DO SEGMENT
CREATE TABLE FLIGHT_LEG (
    ID INT NOT NULL,
    FLIGHT_NUMBER VARCHAR2(255) NULL,
    FLIGHT_DURATION INT NULL,
    AIRLINE VARCHAR2(255) NULL,
    MANAGED_BY VARCHAR2(255) NULL,
    TIME_TO_WAIT INT,
    ORIGIN_IATA VARCHAR2(3) NULL,
    ORIGIN_DESCRIPTION VARCHAR2(255) NULL,
    DESTINATION_IATA VARCHAR2(3) NULL,
    DESTINATION_DESCRIPTION VARCHAR2(255) NULL,
    DEPARTURE_DATE TIMESTAMP WITH TIME ZONE,
    ARRIVAL_DATE TIMESTAMP WITH TIME ZONE,
    TYPE VARCHAR2(50) NULL,
    SEGMENT_ID INT NULL,
    CREATE_DATE TIMESTAMP WITH TIME ZONE,
    LAST_MODIFIED_DATE TIMESTAMP WITH TIME ZONE
);
CREATE SEQUENCE FLIGHT_LEG_SEQ START WITH 1;
CREATE INDEX FLIGHT_LEG_SEGMENT_ID_INDEX on FLIGHT_LEG(SEGMENT_ID) online;
ALTER TABLE FLIGHT_LEG ADD CONSTRAINT PK_FLIGHT_LEG_ID PRIMARY KEY (ID) ENABLE;
ALTER TABLE FLIGHT_LEG ADD CONSTRAINT fk_flight_leg_segment_id FOREIGN KEY (SEGMENT_ID) REFERENCES SEGMENT (ID);
COMMENT ON COLUMN FLIGHT_LEG.FLIGHT_NUMBER is 'NUMERO DO VÔO';
COMMENT ON COLUMN FLIGHT_LEG.FLIGHT_DURATION is 'DURAÇÃO DO VÔO EM MINUTOS';
COMMENT ON COLUMN FLIGHT_LEG.AIRLINE is 'COMPANHIA AEREA RESPONSÁVEL PELA OPERAÇÃO';
COMMENT ON COLUMN FLIGHT_LEG.MANAGED_BY is 'COMPANHIA AÉREA RESPONSÁVEL PELA VENDA';
COMMENT ON COLUMN FLIGHT_LEG.TIME_TO_WAIT is 'TEMPO DE ESPERA NA PARADA/CONEXÃO';
COMMENT ON COLUMN FLIGHT_LEG.ORIGIN_IATA is 'IADA DE ORIGEM';
COMMENT ON COLUMN FLIGHT_LEG.ORIGIN_DESCRIPTION is 'DESCRIÇÃO IATA ORIGEM';
COMMENT ON COLUMN FLIGHT_LEG.DESTINATION_IATA is 'IATA DESTINO';
COMMENT ON COLUMN FLIGHT_LEG.DESTINATION_DESCRIPTION is 'DESCRIÇÃO IATA DESTINO';
COMMENT ON COLUMN FLIGHT_LEG.DEPARTURE_DATE is 'DATA E HORA DE PARTIDA DA DECOLAGEM';
COMMENT ON COLUMN FLIGHT_LEG.ARRIVAL_DATE is 'DATA E HORA DO POUSO';
COMMENT ON COLUMN FLIGHT_LEG.TYPE is 'TIPO DE LEG (INITIAL/STOP,FINAL)';

CREATE TABLE LUGGAGE (
    ID INT NOT NULL,
    DESCRIPTION VARCHAR2(255) NULL,
    TYPE VARCHAR2(50) NOT NULL,
    SEGMENT_ID INT NULL,
    CREATE_DATE TIMESTAMP WITH TIME ZONE,
    LAST_MODIFIED_DATE TIMESTAMP WITH TIME ZONE
);
CREATE SEQUENCE LUGGAGE_SEQ START WITH 1;
CREATE INDEX LUGGAGE_SEGMENT_ID_INDEX on LUGGAGE(SEGMENT_ID) online;
ALTER TABLE LUGGAGE ADD CONSTRAINT PK_LUGGAGE_ID PRIMARY KEY (ID) ENABLE;
ALTER TABLE LUGGAGE ADD CONSTRAINT fk_luggage_segment_id FOREIGN KEY (SEGMENT_ID) REFERENCES SEGMENT (ID);
COMMENT ON COLUMN LUGGAGE.DESCRIPTION is 'DESCRIÇÃO DA BAGAGEM';
COMMENT ON COLUMN LUGGAGE.TYPE is 'TIPO (HAND,BAGGAGE)';

CREATE TABLE CANCELATION_RULE (
    ID INT NOT NULL,
    DESCRIPTION VARCHAR2(255) NULL,
    TYPE VARCHAR2(50) NOT NULL,
    SEGMENT_ID INT,
    CREATE_DATE TIMESTAMP WITH TIME ZONE,
    LAST_MODIFIED_DATE TIMESTAMP WITH TIME ZONE
);
CREATE SEQUENCE CANCELATION_RULE_SEQ START WITH 1;
CREATE INDEX CANCELATION_RULE_SEGMENT_ID_INDEX on CANCELATION_RULE(SEGMENT_ID) online;
ALTER TABLE CANCELATION_RULE ADD CONSTRAINT PK_CANCELATION_RULE_ID PRIMARY KEY (ID) ENABLE;
ALTER TABLE CANCELATION_RULE ADD CONSTRAINT fk_cancelation_rule_segment_id FOREIGN KEY (SEGMENT_ID) REFERENCES SEGMENT (ID);
COMMENT ON COLUMN CANCELATION_RULE.DESCRIPTION is 'DESCRIÇÃO DA REGRA DE CANCELAMENTO NO PARCEIRO';
COMMENT ON COLUMN CANCELATION_RULE.TYPE is 'TIPO DE REGRA DE CANCELAMENTO NO PARCEIRO **PENDENTE DE NORMALIZAÇÃO LIVELO';

CREATE TABLE CHANGE_RULE (
    ID INT NOT NULL,
    DESCRIPTION VARCHAR2(255) NULL,
    TYPE VARCHAR2(50) NOT NULL,
    SEGMENT_ID INT,
    CREATE_DATE TIMESTAMP WITH TIME ZONE,
    LAST_MODIFIED_DATE TIMESTAMP WITH TIME ZONE
);
CREATE SEQUENCE CHANGE_RULE_SEQ START WITH 1;
CREATE INDEX CHANGE_RULE_SEGMENT_ID_INDEX on CHANGE_RULE(SEGMENT_ID) online;
ALTER TABLE CHANGE_RULE ADD CONSTRAINT PK_CHANGE_RULE_ID PRIMARY KEY (ID) ENABLE;
ALTER TABLE CHANGE_RULE ADD CONSTRAINT fk_change_rule_segment_id FOREIGN KEY (SEGMENT_ID) REFERENCES SEGMENT (ID);
COMMENT ON COLUMN CHANGE_RULE.DESCRIPTION is 'DESCRIÇÃO DA REGRA DE ALTERAÇÃO NO PARCEIRO';
COMMENT ON COLUMN CHANGE_RULE.TYPE is 'TIPO DA REGRA DE ALTERAÇÃO **PENDENTE DE NORMALIZAÇÃO LIVELO';