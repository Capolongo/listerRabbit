ALTER TABLE SEGMENT DROP COLUMN PARTNER_ID;

CREATE TABLE PRICE_MODALITY (
                                ID INT NOT NULL,
                                AMOUNT NUMBER(10,2),
                                POINTS_AMOUNT NUMBER(10,2),
                                ACCRUAL_POINTS NUMBER(10,2),
                                PRICE_LIST_ID VARCHAR(255),
                                ORDER_ITEM_PRICE_ID INT NOT NULL
);
CREATE SEQUENCE PRICE_MODALITY_SEQ START WITH 1;
ALTER TABLE PRICE_MODALITY ADD CONSTRAINT PK_PRICE_MODALITY_ID PRIMARY KEY (ID) ENABLE;
CREATE INDEX ORDER_PRICE_ID_PRICE_MODALITY_INDEX on PRICE_MODALITY(ORDER_ITEM_PRICE_ID) online;
ALTER TABLE PRICE_MODALITY ADD CONSTRAINT fk_price_modality_order_item_price_id FOREIGN KEY (ORDER_ITEM_PRICE_ID) REFERENCES ORDERS_ITEM_PRICE (ID);
COMMENT ON COLUMN PRICE_MODALITY.PRICE_LIST_ID is 'id da regra de preço vinda da calculadora';

ALTER TABLE SEGMENT ADD PARTNER_ID CLOB;