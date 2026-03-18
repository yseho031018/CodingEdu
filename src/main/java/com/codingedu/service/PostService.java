package com.codingedu.service;

import com.codingedu.entity.Post;
import com.codingedu.entity.User;
import com.codingedu.repository.PostRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class PostService {

    private final PostRepository postRepository;

    public PostService(PostRepository postRepository) {
        this.postRepository = postRepository;
    }

    public List<Post> getPostsByCategory(String category) {
        if ("all".equals(category)) {
            return postRepository.findAllByOrderByCreatedAtDesc();
        }
        return postRepository.findByCategoryOrderByCreatedAtDesc(category);
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
    public void incrementCommentCount(Post post) {
        post.setCommentCount(post.getCommentCount() + 1);
        postRepository.save(post);
    }
}
