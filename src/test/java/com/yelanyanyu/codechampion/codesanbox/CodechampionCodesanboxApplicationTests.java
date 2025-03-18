package com.yelanyanyu.codechampion.codesanbox;

import cn.hutool.core.io.resource.ResourceUtil;
import com.yelanyanyu.codechampion.codesanbox.model.ExecuteCodeRequest;
import com.yelanyanyu.codechampion.codesanbox.model.ExecuteCodeResponse;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

@SpringBootTest
class CodechampionCodesanboxApplicationTests {
	@Resource
	private JavaNativeCodeSandbox javaNativeCodeSandbox;

	@Test
	void contextLoads() {
		ExecuteCodeRequest executeCodeRequest = new ExecuteCodeRequest();
		executeCodeRequest.setInput(Arrays.asList("1 2", "3 4"));
		String code = ResourceUtil.readStr("simpleComputeArgs/Main.java", StandardCharsets.UTF_8);
		executeCodeRequest.setCode(code);
		executeCodeRequest.setLanguage("java");
		ExecuteCodeResponse executeCodeResponse = javaNativeCodeSandbox.execute(executeCodeRequest);
		System.out.println(executeCodeResponse);
	}

}
