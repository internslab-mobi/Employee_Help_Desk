-- Add reply_to_message_id column to hd_ticket_messages table for message threading
ALTER TABLE hd_ticket_messages
ADD COLUMN reply_to_message_id BIGINT,
ADD CONSTRAINT fk_message_reply
FOREIGN KEY (reply_to_message_id) REFERENCES hd_ticket_messages(id);
