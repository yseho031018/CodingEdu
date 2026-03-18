package com.codingedu.controller;

import com.codingedu.entity.Post;
import com.codingedu.entity.User;
import com.codingedu.security.CustomUserDetails;
import com.codingedu.service.CommentService;
import com.codingedu.service.PostService;
import com.codingedu.service.UserService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class CommentController {

    private final CommentService commentService;
    private final PostService postService;
    private final UserService userService;

    public CommentController(CommentService commentService, PostService postService, UserService userService) {
        this.commentService = commentService;
        this.postService = postService;
        this.userService = userService;
    }

    // 댓글 등록
    @PostMapping("/community/{postId}/comment")
    public String addComment(@PathVariable(name = "postId") Long postId,
                             @RequestParam(name = "content") String content,
                             @AuthenticationPrincipal CustomUserDetails userDetails) {
        if (userDetails == null) {
            return "redirect:/login";
        }

        Post post = postService.getPostById(postId);
        User author = userService.findByUsername(userDetails.getUsername());
        commentService.addComment(content, post, author);

        return "redirect:/community/" + postId;
    }
}
