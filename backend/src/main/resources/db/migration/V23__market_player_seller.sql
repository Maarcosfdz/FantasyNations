-- User listings on the free market.
-- seller_user_id NULL  -> system listing (free market refreshed each cycle).
-- seller_user_id set   -> user listing, lasts 48h, transfer credits the seller.

ALTER TABLE market_players
    ADD COLUMN seller_user_id UUID REFERENCES users(id);

CREATE INDEX idx_market_players_seller ON market_players(seller_user_id);
