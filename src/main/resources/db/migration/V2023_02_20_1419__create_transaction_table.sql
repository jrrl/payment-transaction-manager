
create table if not exists payment_transaction (
    id UUID not null,
    amount NUMERIC(20, 8) not null,
    currency VARCHAR(4) not null,
    type VARCHAR(63) not null,
    status VARCHAR(63) not null,
    --sender
    sender_customer_id UUID not null,
    sender_account_id UUID not null,
    sender_account_number VARCHAR(63) not null,
    --
    fraud_status VARCHAR(63) not null,
    -- customer fees
    customer_fee_amount NUMERIC(20, 8) not null,
    customer_fee_currency VARCHAR(4) not null,
    customer_fee_subscription_id VARCHAR(63) null,
    customer_fee_id VARCHAR(63) null,
    customer_fee_posting_id VARCHAR(63) null,
    -- vendor fees
    vendor_fee_amount NUMERIC(20, 8) not null,
    vendor_fee_currency VARCHAR(4) not null,
    vendor_fee_posting_id VARCHAR(63) null,
    vendor_fee_id VARCHAR(63) null,
    -- provider details
    provider_name VARCHAR(63) not null,
    provider_merchant_code VARCHAR(63) not null,
    provider_merchant_name VARCHAR(255) not null,
    provider_id VARCHAR(63) null,
    provider_status VARCHAR(63) not null,
    -- posting details
    posting_batch_id VARCHAR(63) null,
    posting_id VARCHAR(63) null,
    posted_at TIMESTAMP WITH TIME ZONE null,
    posting_status VARCHAR(63) null,
    created_at TIMESTAMP WITH TIME ZONE not null,
    updated_at TIMESTAMP WITH TIME ZONE not null,
    version BIGINT default 1 not null,
    constraint PK_payment_transactions primary key (id)
);