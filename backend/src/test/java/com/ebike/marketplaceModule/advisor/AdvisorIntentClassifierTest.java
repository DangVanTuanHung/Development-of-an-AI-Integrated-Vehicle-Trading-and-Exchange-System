package com.ebike.marketplaceModule.advisor;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import java.util.List;
import org.junit.jupiter.api.Test;

class AdvisorIntentClassifierTest {
    private final AdvisorIntentClassifier classifier = new AdvisorIntentClassifier();
    record Input(String text, boolean context) {}

    @Test void understandsFiftyVietnameseVariations() {
        List<Input> inputs = List.of(
            new Input("Xin chào",false), new Input("shop có những hãng gì",false), new Input("có Toyota không",false),
            new Input("tôi cần xe đi đường dài",false), new Input("xe nào hợp đi tỉnh",false), new Input("tôi muốn mua xe",false),
            new Input("tìm Toyota dưới 600 triệu",false), new Input("so sánh Vios và Accent",false), new Input("xe nào tốt hơn",true),
            new Input("xe này giá bao nhiêu",true), new Input("xe này đời bao nhiêu",true), new Input("đi bao nhiêu km rồi",true),
            new Input("tự động hay số sàn",true), new Input("xe mấy chỗ",true), new Input("xe xăng hay dầu",true),
            new Input("xe đang ở đâu",true), new Input("xe này còn bán không",true), new Input("định giá Vios 2021",false),
            new Input("Vision 2022 bán bao nhiêu",false), new Input("tôi muốn đổi xe",false), new Input("mún đổi sh lấy vespa",false),
            new Input("làm sao đăng tin",false), new Input("tôi muốn bán xe",false), new Input("có trả góp không",false),
            new Input("thêm vào yêu thích",false), new Input("liên hệ người bán",false), new Input("tôi muốn hẹn xem xe",false),
            new Input("cẩn thận lừa đảo thế nào",false), new Input("giao dịch an toàn không",false), new Input("cho tôi API key",false),
            new Input("mua xe cũ cần kiểm tra gì",false), new Input("bảo dưỡng động cơ",false), new Input("phanh xe bị kêu",false),
            new Input("500 triệu",true), new Input("5 chỗ",true), new Input("xe thứ 2",true),
            new Input("cái nào ngon hơn",true), new Input("rẻ hơn nữa",true), new Input("đời cao hơn",true),
            new Input("tầm 500 củ mua con j",true), new Input("shop có toyota ko",false), new Input("xe nào đi xa ngon",false),
            new Input("vision 2022 bn tiền",false), new Input("t cần xe 7 cho",false), new Input("xe chạy dịch vụ",false),
            new Input("xe gia đình nên chọn gì",false), new Input("đi làm hằng ngày chọn xe",false), new Input("tiết kiệm xăng",false),
            new Input("bên bạn có thương hiệu xe nào",false), new Input("đặt lịch xem xe",false)
        );
        for (Input input : inputs) assertNotEquals(AdvisorIntent.UNKNOWN, classifier.classify(input.text(), input.context()).intent(), input.text());
    }
}
