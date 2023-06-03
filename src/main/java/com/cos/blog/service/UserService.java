package com.cos.blog.service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.validation.BindingResult;
import org.springframework.validation.Errors;
import org.springframework.validation.FieldError;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;
import com.cos.blog.dto.UserRequestDto;
import com.cos.blog.model.RoleType;
import com.cos.blog.model.User;
import com.cos.blog.repository.UserRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserService {

	private final UserRepository userRepository;
	private final BCryptPasswordEncoder encoder;
	private final AuthenticationManager authenticationManager;



	@Value("${secrect.key}")
	private String secrectKey;

	@Value("${file.path}")
	private String uploadFolder;



	@Transactional
	public void join(UserRequestDto userDto) {
		User user = User.builder()
				.username(userDto.getUsername())
				.password(encoder.encode(userDto.getPassword()))
				.nickname(userDto.getNickname())
				.email(userDto.getEmail())
				.role(RoleType.USER)
				.build();

		userRepository.save(user);
	}

	@Transactional
	public void join(User user) {
		userRepository.save(user);
	}

	@Transactional
	public void update(UserRequestDto userDto) {
		User persistance = userRepository.findByUsername(userDto.getUsername()).orElseThrow(() -> {
			return new IllegalArgumentException("회원정보 수정 실패: 존재하지 않는 회원입니다.");
		});

		persistance.setPassword(encoder.encode(userDto.getPassword()));
		persistance.setNickname(userDto.getNickname());
	}

	@Transactional
	public void delete(Long user_id) {
		userRepository.deleteById(user_id);
	}

	@Transactional(readOnly = true)
	public Map<String, String> validateHandling(BindingResult bindingResult) {
		Map<String, String> validatorResult = new HashMap<>();

		for (FieldError error : bindingResult.getFieldErrors()) {
			String validKeyName = String.format("valid_%s", error.getField());
			validatorResult.put(validKeyName, error.getDefaultMessage());
		}

		return validatorResult;
	}

	@Transactional(readOnly = true)
	public Map<String, String> validateHandling(Errors errors) {
		Map<String, String> validatorResult = new HashMap<>();

		for (FieldError error : errors.getFieldErrors()) {
			String validKeyName = String.format("valid_%s", error.getField());
			validatorResult.put(validKeyName, error.getDefaultMessage());
		}

		return validatorResult;
	}

	@Transactional
	public void profileImageUpdate(Long user_id, MultipartFile profileImageFile) {
		UUID uuid = UUID.randomUUID();
		String imageFileName = uuid + "_" + profileImageFile.getOriginalFilename();
		Path imageFilePath = Paths.get(uploadFolder + imageFileName);

		try {
			Files.write(imageFilePath, profileImageFile.getBytes());
		} catch (Exception e) {
			e.printStackTrace();
		}

		User user = userRepository.findById(user_id).orElseThrow(() -> {
			return new IllegalArgumentException("프로필 이미지 수정 실패: 존재하지 않는 회원입니다.");
		});

		user.setProfile_image_url(imageFileName);
	}
}

