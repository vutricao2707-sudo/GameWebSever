-- =====================================================================
-- WATCHLIST TABLE MIGRATION
-- Creates watchlist functionality for users to track license plates
-- =====================================================================

-- Create watchlist table
CREATE TABLE IF NOT EXISTS watchlist (
    id INT PRIMARY KEY AUTO_INCREMENT,
    user_id INT NOT NULL,
    item_id INT NOT NULL,
    plate_number VARCHAR(20),
    added_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    notification_sent BOOLEAN DEFAULT FALSE,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (item_id) REFERENCES items(id) ON DELETE CASCADE,
    UNIQUE KEY unique_watchlist (user_id, item_id)
);

-- Create index for faster queries
CREATE INDEX idx_watchlist_user ON watchlist(user_id);
CREATE INDEX idx_watchlist_item ON watchlist(item_id);
CREATE INDEX idx_watchlist_notification ON watchlist(notification_sent);

-- Sample query to view all watchlist items for a user
-- SELECT w.*, i.plate_number, i.status, i.auction_status
-- FROM watchlist w
-- JOIN items i ON w.item_id = i.id
-- WHERE w.user_id = ?
-- ORDER BY w.added_at DESC;

-- Sample query to get unwatched items that entered REGISTRATION phase
-- SELECT w.id, w.user_id, i.plate_number, i.auction_status
-- FROM watchlist w
-- JOIN items i ON w.item_id = i.id
-- WHERE w.notification_sent = FALSE
-- AND i.auction_status = 'REGISTRATION'
-- AND w.user_id = ?;

