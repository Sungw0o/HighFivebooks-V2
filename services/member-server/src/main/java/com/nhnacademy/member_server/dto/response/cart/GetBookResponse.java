package com.nhnacademy.member_server.dto.response.cart;

public record GetBookResponse(Long bookId,
                              String title,
                              Integer price,
                              String image) {}