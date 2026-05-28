package com.yx.lab.modules.detection.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class DetectionMethodInstrumentModelBindCommand {

    private List<Item> items = new ArrayList<>();

    @Data
    public static class Item {

        private String instrumentModel;

        private String manufacturer;

        private String remark;
    }
}
