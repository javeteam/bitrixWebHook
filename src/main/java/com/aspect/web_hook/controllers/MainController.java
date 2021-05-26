package com.aspect.web_hook.controllers;

import com.aspect.web_hook.entity.ExpandiLead;
import com.aspect.web_hook.service.BitrixService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

@Controller
public class MainController {
    private final BitrixService bitrixService;

    @Autowired
    public MainController(BitrixService bitrixService) {
        this.bitrixService = bitrixService;
    }

    @RequestMapping(value = {"/api/p3hX8OKa8bOXTtT"}, method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public String getImportLead(@RequestBody ExpandiLead lead){
        this.bitrixService.addLead(lead);
        return "{\"Status\":\"Success\"}";
    }

    @RequestMapping(value = {"/api/kjsMGkjiZTOZQqD"}, method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public String createBitrixTask(@RequestBody ExpandiLead lead){
        this.bitrixService.createTask(lead);
        return "{\"Status\":\"Success\"}";
    }





}
