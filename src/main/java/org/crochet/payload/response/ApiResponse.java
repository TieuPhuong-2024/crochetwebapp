package org.crochet.payload.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ApiResponse<T> {
    private T data;
    private String message;
    private boolean success = true;
    
    public ApiResponse(T data, String message) {
        this.data = data;
        this.message = message;
    }
} 