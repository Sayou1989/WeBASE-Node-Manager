/**
 * Copyright 2014-2020  the original author or authors.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package com.webank.webase.node.mgr.deploy.controller;

import java.io.IOException;
import java.time.Instant;

import javax.validation.Valid;

import org.apache.commons.lang3.tuple.Pair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.webank.webase.node.mgr.base.code.ConstantCode;
import com.webank.webase.node.mgr.base.code.RetCode;
import com.webank.webase.node.mgr.base.controller.BaseController;
import com.webank.webase.node.mgr.base.entity.BaseResponse;
import com.webank.webase.node.mgr.base.enums.FrontStatusEnum;
import com.webank.webase.node.mgr.base.enums.OptionType;
import com.webank.webase.node.mgr.base.exception.NodeMgrException;
import com.webank.webase.node.mgr.base.properties.ConstantProperties;
import com.webank.webase.node.mgr.base.tools.JsonTools;
import com.webank.webase.node.mgr.deploy.entity.ReqAdd;
import com.webank.webase.node.mgr.deploy.entity.ReqDeploy;
import com.webank.webase.node.mgr.deploy.entity.ReqNodeOption;
import com.webank.webase.node.mgr.deploy.entity.ReqUpgrade;
import com.webank.webase.node.mgr.deploy.entity.TbChain;
import com.webank.webase.node.mgr.deploy.mapper.TbChainMapper;
import com.webank.webase.node.mgr.deploy.mapper.TbHostMapper;
import com.webank.webase.node.mgr.deploy.service.DeployService;
import com.webank.webase.node.mgr.scheduler.ResetGroupListTask;

import lombok.extern.log4j.Log4j2;

/**
 * Controller for node data.
 */
@Log4j2
@RestController
@RequestMapping("deploy")
public class DeployController extends BaseController {

    @Autowired private TbChainMapper tbChainMapper;
    @Autowired private TbHostMapper tbHostMapper;

    @Autowired private DeployService deployService;
    @Autowired private ResetGroupListTask resetGroupListTask;
    @Autowired private ConstantProperties constantProperties;

    /**
     * Deploy by ipconf and tagId.
     */
    @PostMapping(value = "init")
    @PreAuthorize(ConstantProperties.HAS_ROLE_ADMIN)
    public BaseResponse deployChain(@RequestBody @Valid ReqDeploy deploy,
                               BindingResult result) throws NodeMgrException {
        checkBindResult(result);
        Instant startTime = Instant.now();
        deploy.setWebaseSignAddr(constantProperties.getWebaseSignAddress());
        deploy.setRootDirOnHost(constantProperties.getRootDirOnHost());
        log.info("Start deploy:[{}], start:[{}]", JsonTools.toJSONString(deploy), startTime);

        try {
            // generate node config and return shell execution log
            this.deployService.deployChain(deploy.getChainName(),
                    deploy.getIpconf(), deploy.getTagId(), deploy.getRootDirOnHost(),
                    deploy.getWebaseSignAddr(),deploy.getDockerImageType());

            return new BaseResponse(ConstantCode.SUCCESS);
        } catch (NodeMgrException e) {
            return new BaseResponse(e.getRetCode());
        }
    }

    /**
     *
     * @param add
     * @param result
     * @return
     * @throws NodeMgrException
     */
    @PostMapping(value = "node/add")
    @PreAuthorize(ConstantProperties.HAS_ROLE_ADMIN)
    public BaseResponse addNode(
            @RequestBody @Valid ReqAdd add,
            BindingResult result) throws NodeMgrException {
        checkBindResult(result);
        Instant startTime = Instant.now();

        log.info("Start add node:[{}] , start[{}]", JsonTools.toJSONString(add), startTime);

        Pair<RetCode, String> addResult = this.deployService.addNodes(add);
        return new BaseResponse(addResult.getKey(), addResult.getValue());
    }

    /**
     *
     * @param start
     * @param result
     * @return
     * @throws NodeMgrException
     */
    @PostMapping(value = "node/start")
    @PreAuthorize(ConstantProperties.HAS_ROLE_ADMIN)
    public BaseResponse startNode(
            @RequestBody @Valid ReqNodeOption start, BindingResult result) throws NodeMgrException {
        checkBindResult(result);
        String nodeId = start.getNodeId();
        Instant startTime = Instant.now();

        log.info("Start node nodeId:[{}], now:[{}]", nodeId, startTime);

        this.deployService.startNode(start.getNodeId(), OptionType.MODIFY_CHAIN, FrontStatusEnum.STOPPED,
                FrontStatusEnum.RUNNING, FrontStatusEnum.STOPPED);
        return new BaseResponse(ConstantCode.SUCCESS);
    }

    /**
     *
     * @param stop
     * @param result
     * @return
     * @throws NodeMgrException
     */
    @PostMapping(value = "node/stop")
    @PreAuthorize(ConstantProperties.HAS_ROLE_ADMIN)
    public BaseResponse stopNode(
            @RequestBody @Valid ReqNodeOption stop, BindingResult result) throws NodeMgrException {
        checkBindResult(result);
        String nodeId = stop.getNodeId();
        Instant startTime = Instant.now();

        log.info("Stop node nodeId:[{}], now:[{}]", nodeId, startTime);

        this.deployService.stopNode(stop.getNodeId());
        return new BaseResponse(ConstantCode.SUCCESS);
    }

    /**
     *
     * @param delete
     * @param result
     * @return
     * @throws NodeMgrException
     */
    @PostMapping(value = "node/delete")
    @PreAuthorize(ConstantProperties.HAS_ROLE_ADMIN)
    public BaseResponse deleteNode(
            @RequestBody @Valid ReqNodeOption delete, BindingResult result) throws NodeMgrException {
        checkBindResult(result);
        String nodeId = delete.getNodeId();
        Instant startTime = Instant.now();

        log.info("Delete node nodeId:[{}], now:[{}]", nodeId, startTime);

        this.deployService.deleteNode(delete.getNodeId(),
                delete.isDeleteHost(),delete.isDeleteAgency());
        return new BaseResponse(ConstantCode.SUCCESS);
    }

    /**
     *
     * @param upgrade
     * @param result
     * @return
     * @throws IOException
     */
    @PostMapping(value = "upgrade")
    @PreAuthorize(ConstantProperties.HAS_ROLE_ADMIN)
    public BaseResponse upgradeChain(
            @RequestBody @Valid ReqUpgrade upgrade, BindingResult result ) throws IOException {
        checkBindResult(result);
        int newTagId = upgrade.getNewTagId();
        String chainName = upgrade.getChainName();
        Instant startTime = Instant.now();
        log.info("Start upgrade chain to version:[{}], chainName:[{}], now:[{}]", newTagId, chainName, startTime);
        this.deployService.upgrade(newTagId,chainName);
        return new BaseResponse(ConstantCode.SUCCESS);
    }

    /**
     *
     * @return
     * @throws IOException
     */
    @GetMapping(value = "progress")
    @PreAuthorize(ConstantProperties.HAS_ROLE_ADMIN)
    public BaseResponse progress(
            @RequestParam(value = "chainName", required = false, defaultValue = "default_chain") String chainName
         ) throws IOException {
        Instant startTime = Instant.now();
        log.info("Start get progress, chainName:[{}], now:[{}]", chainName, startTime);
        int progress = this.deployService.progress(chainName);
        return new BaseResponse(ConstantCode.SUCCESS, progress);
    }

    /**
     *
     * @param chainName
     * @return
     * @throws IOException
     */
    @GetMapping(value = "chain/info")
    public BaseResponse getChain(
            @RequestParam(value = "chainName", required = false, defaultValue = "default_chain") String chainName
    ) throws IOException {
        Instant startTime = Instant.now();
        log.info("Start get chain info chainName:[{}], now:[{}]", chainName, startTime);

        TbChain chain = this.tbChainMapper.getByChainName(chainName);
        return new BaseResponse(ConstantCode.SUCCESS, chain);
    }


    /**
     *
     * @param chainName
     * @return
     * @throws IOException
     */
    @GetMapping(value = "chain/start")
    @PreAuthorize(ConstantProperties.HAS_ROLE_ADMIN)
    public BaseResponse startChain(
            @RequestParam(value = "chainName", required = false, defaultValue = "default_chain") String chainName
    ) throws IOException {
        Instant startTime = Instant.now();
        log.info("Start chain, chainName:[{}], now:[{}]", chainName, startTime);

        return new BaseResponse(ConstantCode.SUCCESS );
    }

    /**
     *
     * @param chainName
     * @return
     * @throws IOException
     */
    @GetMapping(value = "chain/stop")
    @PreAuthorize(ConstantProperties.HAS_ROLE_ADMIN)
    public BaseResponse stopChain(
            @RequestParam(value = "chainName", required = false, defaultValue = "default_chain") String chainName
    ) throws IOException {
        Instant startTime = Instant.now();
        log.info("Stop chain, chainName:[{}], now:[{}]", chainName, startTime);

        return new BaseResponse(ConstantCode.SUCCESS);
    }

    /**
     * delete chain by chainName.
     */
    @DeleteMapping(value = "delete")
    @PreAuthorize(ConstantProperties.HAS_ROLE_ADMIN)
    public BaseResponse deleteChain(
            @RequestParam(value = "chainName", required = false, defaultValue = "default_chain") String chainName
    ) throws NodeMgrException {
        Instant startTime = Instant.now();
        log.info("Start delete chainName:[{}], startTime:[{}]",
                chainName, startTime.toEpochMilli());

        RetCode deleteResult = this.deployService.deleteChain(chainName);
        return new BaseResponse(deleteResult);
    }

    /**
     *
     * @return
     * @throws IOException
     */
    @GetMapping(value = "type")
    public BaseResponse deployType() throws IOException {
        Instant startTime = Instant.now();
        log.info("Start get deploy type, now:[{}]",  startTime);
        return new BaseResponse(ConstantCode.SUCCESS, constantProperties.getDeployType());
    }

    /**
     *
     * @return
     * @throws IOException
     */
    @GetMapping(value = "host/list")
    public BaseResponse listHost() throws IOException {
        Instant startTime = Instant.now();
        log.info("Start get host list info, now:[{}]",  startTime);
        return new BaseResponse(ConstantCode.SUCCESS, this.tbHostMapper.selectAll());
    }
}
