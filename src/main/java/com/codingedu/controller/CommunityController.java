package com.codingedu.controller;

import com.codingedu.entity.Post;
import com.codingedu.entity.User;
import com.codingedu.security.CustomUserDetails;
import com.codingedu.service.CommentService;
import com.codingedu.service.PostService;
import com.codingedu.service.UserService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/community")
public class CommunityController {

    private final PostService postService;
    private final UserService userService;
    private final CommentService commentService;

    public CommunityController(PostService postService, UserService userService, CommentService commentService) {
        this.postService = postService;
        this.userService = userService;
        this.commentService = commentService;
    }

    // 1. 커뮤니티 목록 보기
    @GetMapping
    public String list(@RequestParam(name = "category", required = false, defaultValue = "all") String category, Model model) {
        model.addAttribute("posts", postService.getPostsByCategory(category));
        model.addAttribute("currentCategory", category);
        return "community";
    }

    // 2. 글 작성 폼 보여주기
    @GetMapping("/write")
    public String writeForm(@AuthenticationPrincipal CustomUserDetails userDetails) {
        if (userDetails == null) {
            return "redirect:/login";
        }
        return "community-write";
    }

    // 3. 폼 제출 시 실제 DB에 글 저장하기
    @PostMapping("/write")
    public String writeProcess(@RequestParam(name = "category") String category,
                               @RequestParam(name = "title") String title,
                               @RequestParam(name = "content") String content,
                               @AuthenticationPrincipal CustomUserDetails userDetails) {
        if (userDetails == null) {
            return "redirect:/login";
        }

        User author = userService.findByUsername(userDetails.getUsername());
        postService.createPost(category, title, content, author);
        return "redirect:/community";
    }

    // 4. 글 상세보기 (조회수 증가 포함)
    @GetMapping("/{id}")
    public String detail(@PathVariable(name = "id") Long id, Model model) {
        Post post = postService.getPostAndIncreaseViews(id);
        model.addAttribute("post", post);
        model.addAttribute("comments", commentService.getCommentsByPostId(id));
        return "community-detail";
    }
}
