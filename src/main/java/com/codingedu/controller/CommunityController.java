package com.codingedu.controller;

import com.codingedu.entity.Post;
import com.codingedu.entity.User;
import com.codingedu.security.CustomUserDetails;
import com.codingedu.service.CommentService;
import com.codingedu.service.PostService;
import com.codingedu.service.UserService;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

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

    // 1. 커뮤니티 목록 보기 (페이지네이션 + 검색)
    @GetMapping
    public String list(@RequestParam(name = "category", defaultValue = "all") String category,
                       @RequestParam(name = "page", defaultValue = "0") int page,
                       @RequestParam(name = "keyword", defaultValue = "") String keyword,
                       Model model) {
        Page<Post> postPage = keyword.isBlank()
                ? postService.getPostsByCategory(category, page)
                : postService.searchPosts(category, keyword, page);
        model.addAttribute("postPage", postPage);
        model.addAttribute("currentCategory", category);
        model.addAttribute("currentPage", page);
        model.addAttribute("keyword", keyword);
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
    public String detail(@PathVariable(name = "id") Long id,
                         @AuthenticationPrincipal CustomUserDetails userDetails,
                         Model model) {
        Post post = postService.getPostAndIncreaseViews(id);
        model.addAttribute("post", post);
        model.addAttribute("comments", commentService.getCommentsByPostId(id));
        if (userDetails != null) {
            User user = userService.findByUsername(userDetails.getUsername());
            model.addAttribute("currentUsername", userDetails.getUsername());
            model.addAttribute("isLiked", postService.isLikedByUser(post, user));
        }
        return "community-detail";
    }

    // 10. 좋아요 토글 (AJAX)
    @PostMapping("/{id}/like")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> toggleLike(
            @PathVariable(name = "id") Long id,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        if (userDetails == null) {
            return ResponseEntity.status(401).body(Map.of("error", "로그인이 필요합니다."));
        }
        Post post = postService.getPostById(id);
        User user = userService.findByUsername(userDetails.getUsername());
        boolean liked = postService.toggleLike(post, user);
        return ResponseEntity.ok(Map.of("liked", liked, "likeCount", post.getLikeCount()));
    }

    // 5. 글 수정 폼 보여주기
    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable(name = "id") Long id,
                           @AuthenticationPrincipal CustomUserDetails userDetails,
                           Model model) {
        if (userDetails == null) {
            return "redirect:/login";
        }
        Post post = postService.getPostById(id);
        if (!post.getAuthor().getUsername().equals(userDetails.getUsername())) {
            return "redirect:/community/" + id;
        }
        model.addAttribute("post", post);
        return "community-edit";
    }

    // 6. 글 수정 처리
    @PostMapping("/{id}/edit")
    public String editProcess(@PathVariable(name = "id") Long id,
                              @RequestParam(name = "category") String category,
                              @RequestParam(name = "title") String title,
                              @RequestParam(name = "content") String content,
                              @AuthenticationPrincipal CustomUserDetails userDetails) {
        if (userDetails == null) {
            return "redirect:/login";
        }
        postService.updatePost(id, category, title, content, userDetails.getUsername());
        return "redirect:/community/" + id;
    }

    // 7. 글 삭제 처리
    @PostMapping("/{id}/delete")
    public String deletePost(@PathVariable(name = "id") Long id,
                             @AuthenticationPrincipal CustomUserDetails userDetails) {
        if (userDetails == null) {
            return "redirect:/login";
        }
        postService.deletePost(id, userDetails.getUsername());
        return "redirect:/community";
    }

}
