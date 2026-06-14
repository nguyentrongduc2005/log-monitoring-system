package com.vdt.log_monitoring.api.identity.dto;

import java.util.List;

import lombok.Builder;
import lombok.Data;

import com.vdt.log_monitoring.modules.identity.api.IdentityFacade;

@Data
@Builder
public class UserPageResponse {
	private List<UserResponse> users;
	private int page;
	private int size;
	private long totalElements;
	private int totalPages;

	public static UserPageResponse from(IdentityFacade.UserPageDto page) {
		return UserPageResponse.builder()
			.users(page.users().stream().map(UserResponse::from).toList())
			.page(page.page())
			.size(page.size())
			.totalElements(page.totalElements())
			.totalPages(page.totalPages())
			.build();
	}
}
