package com.mini.rpc.consumer.controller;


import com.mini.rpc.consumer.annotation.RpcReference;
import com.mini.rpc.provider.facade.RNMTuiQianFacade;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class RNMController {
    @SuppressWarnings({"SpringJavaAutowiredFieldsWarningInspection", "SpringJavaInjectionPointsAutowiringInspection"})
    @RpcReference(serviceVersion = "1.0.0", timeout = 500)
    private RNMTuiQianFacade tuiqian;

    @RequestMapping(value = "/RNM", method = RequestMethod.GET)
    public String sayRNM() {
        return tuiqian.RNM(100);
    }
}
