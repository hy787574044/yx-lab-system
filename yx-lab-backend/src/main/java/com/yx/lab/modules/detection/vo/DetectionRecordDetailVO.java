package com.yx.lab.modules.detection.vo;

import com.yx.lab.modules.detection.entity.DetectionItem;
import com.yx.lab.modules.detection.entity.DetectionRecord;
import lombok.Data;

import java.util.List;

@Data
public class DetectionRecordDetailVO {

    private DetectionRecord record;

    private List<DetectionItem> items;
}
