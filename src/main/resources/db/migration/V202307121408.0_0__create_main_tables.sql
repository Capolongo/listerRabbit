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

CREATE TABLE ORDERS_ITEM_PRICE (
    ID INT NOT NULL,
    LIST_PRICE VARCHAR2(255) NOT NULL,
    AMOUNT NUMBER(15,4),
    POINTS_AMOUNT NUMBER(10,2),
    ACCRUAL_POINTS NUMBER(10,2),
    PARTNER_AMOUNT NUMBER(10,2),
    PRICE_LIST_ID VARCHAR2(255),
    PRICE_RULE CLOB NULL,
    CREATE_DATE TIMESTAMP WITH TIME ZONE,
    LAST_MODIFIED_DATE TIMESTAMP WITH TIME ZONE
);
CREATE SEQUENCE ORDERS_ITEM_PRICE_SEQ START WITH 1;
ALTER TABLE ORDERS_ITEM_PRICE ADD CONSTRAINT PK_ORDERS_ITEM_PRICE_ID PRIMARY KEY (ID) ENABLE;

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

CREATE TABLE SEGMENT (
    ID INT NOT NULL,
    PARTNER_ID VARCHAR2(255),
    STEP VARCHAR2(255),
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