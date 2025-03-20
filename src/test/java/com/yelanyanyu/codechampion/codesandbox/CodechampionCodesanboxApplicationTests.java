package com.yelanyanyu.codechampion.codesandbox;

import cn.hutool.core.io.resource.ResourceUtil;
import com.yelanyanyu.codechampion.codesandbox.model.ExecuteCodeRequest;
import com.yelanyanyu.codechampion.codesandbox.model.ExecuteCodeResponse;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

@SpringBootTest
class CodechampionCodesandboxApplicationTests {
	@Resource
	private JavaNativeCodeSandbox javaNativeCodeSandbox;

	@Test
	void contextLoads() {
		ExecuteCodeRequest executeCodeRequest = new ExecuteCodeRequest();
		executeCodeRequest.setInput(Arrays.asList("1 2", "3 4"));
		String code = ResourceUtil.readStr("simpleCompute/Main.java", StandardCharsets.UTF_8);
		executeCodeRequest.setCode(code);
		executeCodeRequest.setLanguage("java");
		ExecuteCodeResponse executeCodeResponse = javaNativeCodeSandbox.execute(executeCodeRequest);
		System.out.println(executeCodeResponse);
	}

	@Test
	void t1WithInteraction() {

	}

}
