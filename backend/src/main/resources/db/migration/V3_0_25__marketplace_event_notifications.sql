CREATE TABLE marketplace.reminders(event_key TEXT PRIMARY KEY,created_at TIMESTAMPTZ NOT NULL DEFAULT now());
CREATE FUNCTION marketplace.event_notification() RETURNS TRIGGER LANGUAGE plpgsql AS $$
DECLARE car RECORD; deal RECORD; member RECORD; recipient BIGINT; destination TEXT; note TEXT;
BEGIN
 IF TG_TABLE_NAME='vehicle_listings' THEN
  IF NEW.status IS DISTINCT FROM OLD.status AND NEW.status NOT IN ('DRAFT','PENDING_REVIEW') THEN
   PERFORM marketplace.notify(NEW.seller_id,'Trạng thái tin đăng',NEW.title||': '||NEW.status,'/my-listings',true);
  END IF;
  IF NEW.price IS DISTINCT FROM OLD.price OR (NEW.status IS DISTINCT FROM OLD.status AND NEW.status IN ('SOLD','EXCHANGED')) THEN
   FOR member IN SELECT user_id FROM marketplace.listing_favorites WHERE listing_id=NEW.id LOOP
    PERFORM marketplace.notify(member.user_id,'Tin yêu thích thay đổi',NEW.title||': '||NEW.price||' VND · '||NEW.status,'/listing/'||NEW.public_id,false);
   END LOOP;
  END IF;
 ELSIF TG_TABLE_NAME='offers' THEN
  IF TG_OP='INSERT' THEN
   recipient=CASE WHEN NEW.proposed_by=NEW.buyer_id THEN NEW.seller_id ELSE NEW.buyer_id END;
   PERFORM marketplace.notify(recipient,'Đề nghị mua / trao đổi mới','Có đề nghị mới hoặc mức thương lượng mới cần phản hồi.','/deals',true);
  ELSIF NEW.status IS DISTINCT FROM OLD.status THEN
   PERFORM marketplace.notify(NEW.buyer_id,'Cập nhật đề nghị','Đề nghị của bạn: '||NEW.status,'/deals',false);
   PERFORM marketplace.notify(NEW.seller_id,'Cập nhật đề nghị','Đề nghị nhận được: '||NEW.status,'/deals',false);
  END IF;
 ELSIF TG_TABLE_NAME='transaction_events' THEN
  SELECT * INTO deal FROM marketplace.transactions WHERE id=NEW.transaction_id;
  destination='/deals/'||deal.public_id;
  PERFORM marketplace.notify(deal.buyer_id,'Cập nhật giao dịch',coalesce(NEW.note,NEW.event_type),destination,true);
  PERFORM marketplace.notify(deal.seller_id,'Cập nhật giao dịch',coalesce(NEW.note,NEW.event_type),destination,true);
 ELSIF TG_TABLE_NAME='messages' THEN
  IF TG_OP='INSERT' THEN
   FOR member IN SELECT user_id FROM marketplace.conversation_members WHERE conversation_id=NEW.conversation_id AND user_id<>NEW.sender_id LOOP
    SELECT '/messages?conversation='||public_id INTO destination FROM marketplace.conversations WHERE id=NEW.conversation_id;
    PERFORM marketplace.notify(member.user_id,'Tin nhắn mới',left(coalesce(NEW.content,'Tệp đính kèm / vị trí'),200),destination,false);
   END LOOP;
  END IF;
 ELSIF TG_TABLE_NAME='appointments' THEN
  IF TG_OP='INSERT' OR NEW.status IS DISTINCT FROM OLD.status OR NEW.revision IS DISTINCT FROM OLD.revision THEN
   PERFORM marketplace.notify(NEW.buyer_id,'Lịch xem xe','Lịch xem xe: '||NEW.status,'/appointments',true);
   PERFORM marketplace.notify(NEW.seller_id,'Lịch xem xe','Lịch xem xe: '||NEW.status,'/appointments',true);
  END IF;
 ELSIF TG_TABLE_NAME='seller_reviews' THEN
  IF TG_OP='INSERT' THEN
   PERFORM marketplace.notify(NEW.seller_id,'Đánh giá sau giao dịch','Bạn nhận được đánh giá '||NEW.rating||' sao.','/seller/'||NEW.seller_id,false);
  END IF;
 ELSIF TG_TABLE_NAME='listing_reports' THEN
  IF NEW.status IS DISTINCT FROM OLD.status AND NEW.status<>'OPEN' THEN
   PERFORM marketplace.notify(NEW.reporter_id,'Kết quả báo cáo tin',coalesce(NEW.resolution_note,NEW.status),'/marketplace-account',true);
  END IF;
 ELSIF TG_TABLE_NAME='ticket_messages' THEN
  SELECT * INTO car FROM ebike_support.support_tickets WHERE id=NEW.ticket_id;
  IF car.user_id IS NOT NULL AND car.user_id<>NEW.author_id THEN
   PERFORM marketplace.notify(car.user_id,'Phản hồi hỗ trợ','Yêu cầu '||car.ticket_code||' có phản hồi mới.','/support',true);
  ELSIF car.user_id=NEW.author_id THEN
   FOR member IN SELECT DISTINCT ur.user_id FROM ebike_auth.user_roles ur JOIN ebike_auth.roles r ON r.id=ur.role_id WHERE r.name IN ('ADMIN','MANAGER') LOOP
    PERFORM marketplace.notify(member.user_id,'Yêu cầu hỗ trợ mới','Yêu cầu '||car.ticket_code||' có phản hồi của khách.','/manager/support',false);
   END LOOP;
  END IF;
 END IF;
 RETURN NEW;
END $$;
CREATE TRIGGER notify_listing AFTER UPDATE ON marketplace.vehicle_listings FOR EACH ROW EXECUTE FUNCTION marketplace.event_notification();
CREATE TRIGGER notify_offer AFTER INSERT OR UPDATE ON marketplace.offers FOR EACH ROW EXECUTE FUNCTION marketplace.event_notification();
CREATE TRIGGER notify_trade AFTER INSERT ON marketplace.transaction_events FOR EACH ROW EXECUTE FUNCTION marketplace.event_notification();
CREATE TRIGGER notify_message AFTER INSERT ON marketplace.messages FOR EACH ROW EXECUTE FUNCTION marketplace.event_notification();
CREATE TRIGGER notify_appointment AFTER INSERT OR UPDATE ON marketplace.appointments FOR EACH ROW EXECUTE FUNCTION marketplace.event_notification();
CREATE TRIGGER notify_review AFTER INSERT ON marketplace.seller_reviews FOR EACH ROW EXECUTE FUNCTION marketplace.event_notification();
CREATE TRIGGER notify_report AFTER UPDATE ON marketplace.listing_reports FOR EACH ROW EXECUTE FUNCTION marketplace.event_notification();
CREATE TRIGGER notify_support AFTER INSERT ON ebike_support.ticket_messages FOR EACH ROW EXECUTE FUNCTION marketplace.event_notification();
