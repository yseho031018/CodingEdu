package com.codingedu.service;

import com.codingedu.entity.Post;
import com.codingedu.entity.PostLike;
import com.codingedu.entity.User;
import com.codingedu.repository.PostLikeRepository;
import com.codingedu.repository.PostRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class PostService {

    private static final int PAGE_SIZE = 10;

    private final PostRepository postRepository;
    private final PostLikeRepository postLikeRepository;

    public PostService(PostRepository postRepository, PostLikeRepository postLikeRepository) {
        this.postRepository = postRepository;
        this.postLikeRepository = postLikeRepository;
    }

    public boolean isLikedByUser(Post post, User user) {
        return postLikeRepository.existsByUserAndPost(user, post);
    }

    // 좋아요 토글 - true: 좋아요 추가, false: 취소
    @Transactional
    public boolean toggleLike(Post post, User user) {
        java.util.Optional<PostLike> existing = postLikeRepository.findByUserAndPost(user, post);
        if (existing.isPresent()) {
            postLikeRepository.delete(existing.get());
            post.setLikeCount(Math.max(0, post.getLikeCount() - 1));
            postRepository.save(post);
            return false;
        } else {
            PostLike like = new PostLike();
            like.setUser(user);
            like.setPost(post);
            postLikeRepository.save(like);
            post.setLikeCount(post.getLikeCount() + 1);
            postRepository.save(post);
            return true;
        }
    }

    public Page<Post> getPostsByCategory(String category, int page) {
        Pageable pageable = PageRequest.of(page, PAGE_SIZE);
        if ("all".equals(category)) {
            return postRepository.findAllByOrderByCreatedAtDesc(pageable);
        }
        return postRepository.findByCategoryOrderByCreatedAtDesc(category, pageable);
    }

    public Page<Post> searchPosts(String category, String keyword, int page) {
        Pageable pageable = PageRequest.of(page, PAGE_SIZE);
        if ("all".equals(category)) {
            return postRepository.searchByKeyword(keyword, pageable);
        }
        return postRepository.searchByCategoryAndKeyword(category, keyword, pageable);
    }

    public Post getPostById(Long id) {
        return postRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 게시글입니다."));
    }

    @Transactional
    public Post getPostAndIncreaseViews(Long id) {
        Post post = getPostById(id);
        post.setViews(post.getViews() + 1);
        return postRepository.save(post);
    }

    @Transactional
    public Post createPost(String category, String title, String content, User author) {
        Post post = new Post();
        post.setCategory(category);
        post.setTitle(title);
        post.setContent(content);
        post.setAuthor(author);
        return postRepository.save(post);
    }

    @Transactional
    public void updatePost(Long id, String category, String title, String content, String username) {
        Post post = getPostById(id);
        if (!post.getAuthor().getUsername().equals(username)) {
            throw new IllegalArgumentException("수정 권한이 없습니다.");
        }
        post.setCategory(category);
        post.setTitle(title);
        post.setContent(content);
        postRepository.save(post);
    }

    @Transactional
    public void deletePost(Long id, String username) {
        Post post = getPostById(id);
        if (!post.getAuthor().getUsername().equals(username)) {
            throw new IllegalArgumentException("삭제 권한이 없습니다.");
        }
        postRepository.delete(post);
    }

    @Transactional
    public void incrementCommentCount(Post post) {
        post.setCommentCount(post.getCommentCount() + 1);
        postRepository.save(post);
    }

    public java.util.List<Post> getRecentPostsByUser(User user) {
        return postRepository.findTop5ByAuthorOrderByCreatedAtDesc(user);
    }

    public int countPostsByUser(User user) {
        return postRepository.countByAuthor(user);
    }

    @Transactional
    public void decrementCommentCount(Post post) {
        post.setCommentCount(Math.max(0, post.getCommentCount() - 1));
        postRepository.save(post);
    }
}
