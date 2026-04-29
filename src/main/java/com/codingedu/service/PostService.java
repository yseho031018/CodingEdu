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
    private static final int MAX_TITLE_LENGTH = 200;
    private static final int MAX_CONTENT_LENGTH = 10000;
    private static final java.util.Set<String> VALID_CATEGORIES =
            java.util.Set.of("qna", "tips", "study", "free");

    private final PostRepository postRepository;
    private final PostLikeRepository postLikeRepository;

    public PostService(PostRepository postRepository, PostLikeRepository postLikeRepository) {
        this.postRepository = postRepository;
        this.postLikeRepository = postLikeRepository;
    }

    public boolean isLikedByUser(Post post, User user) {
        return PostLike.TYPE_LIKE.equals(getReactionType(post, user));
    }

    public boolean isDisappointedByUser(Post post, User user) {
        return PostLike.TYPE_DISAPPOINTED.equals(getReactionType(post, user));
    }

    public String getReactionType(Post post, User user) {
        return postLikeRepository.findByUserAndPost(user, post)
                .map(PostLike::getReactionType)
                .orElse("");
    }

    @Transactional
    public boolean toggleLike(Post post, User user) {
        return toggleReaction(post, user, PostLike.TYPE_LIKE).active();
    }

    @Transactional
    public ReactionResult toggleReaction(Post post, User user, String reactionType) {
        String safeReactionType = validateReactionType(reactionType);
        java.util.Optional<PostLike> existing = postLikeRepository.findByUserAndPost(user, post);
        if (existing.isPresent()) {
            PostLike reaction = existing.get();
            if (safeReactionType.equals(reaction.getReactionType())) {
                decrementReactionCount(post, safeReactionType);
                postLikeRepository.delete(reaction);
                postRepository.save(post);
                return new ReactionResult("", false, post.getLikeCount(), post.getDisappointedCount());
            }
            decrementReactionCount(post, reaction.getReactionType());
            reaction.setReactionType(safeReactionType);
            incrementReactionCount(post, safeReactionType);
            postLikeRepository.save(reaction);
            postRepository.save(post);
            return new ReactionResult(safeReactionType, true, post.getLikeCount(), post.getDisappointedCount());
        }

        PostLike reaction = new PostLike();
        reaction.setUser(user);
        reaction.setPost(post);
        reaction.setReactionType(safeReactionType);
        postLikeRepository.save(reaction);
        incrementReactionCount(post, safeReactionType);
        postRepository.save(post);
        return new ReactionResult(safeReactionType, true, post.getLikeCount(), post.getDisappointedCount());
    }

    public Page<Post> getPostsByCategory(String category, int page) {
        Pageable pageable = PageRequest.of(normalizePage(page), PAGE_SIZE);
        if ("all".equals(category)) {
            return postRepository.findAllByOrderByCreatedAtDesc(pageable);
        }
        validateCategory(category);
        return postRepository.findByCategoryOrderByCreatedAtDesc(category, pageable);
    }

    public Page<Post> searchPosts(String category, String keyword, int page) {
        Pageable pageable = PageRequest.of(normalizePage(page), PAGE_SIZE);
        String safeKeyword = keyword == null ? "" : keyword.trim();
        if ("all".equals(category)) {
            return postRepository.searchByKeyword(safeKeyword, pageable);
        }
        validateCategory(category);
        return postRepository.searchByCategoryAndKeyword(category, safeKeyword, pageable);
    }

    public Post getPostById(Long id) {
        return postRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Post does not exist."));
    }

    @Transactional
    public Post getPostAndIncreaseViews(Long id) {
        Post post = getPostById(id);
        post.setViews(post.getViews() + 1);
        return postRepository.save(post);
    }

    @Transactional
    public Post createPost(String category, String title, String content, User author) {
        String safeCategory = validateCategory(category);
        String safeTitle = requireText(title, "title", MAX_TITLE_LENGTH);
        String safeContent = requireText(content, "content", MAX_CONTENT_LENGTH);
        Post post = new Post();
        post.setCategory(safeCategory);
        post.setTitle(safeTitle);
        post.setContent(safeContent);
        post.setAuthor(author);
        return postRepository.save(post);
    }

    @Transactional
    public void updatePost(Long id, String category, String title, String content, String username) {
        Post post = getPostById(id);
        if (!post.getAuthor().getUsername().equals(username)) {
            throw new IllegalArgumentException("No permission to edit this post.");
        }
        post.setCategory(validateCategory(category));
        post.setTitle(requireText(title, "title", MAX_TITLE_LENGTH));
        post.setContent(requireText(content, "content", MAX_CONTENT_LENGTH));
        postRepository.save(post);
    }

    @Transactional
    public void deletePost(Long id, String username) {
        Post post = getPostById(id);
        if (!post.getAuthor().getUsername().equals(username)) {
            throw new IllegalArgumentException("No permission to delete this post.");
        }
        postRepository.delete(post);
    }

    public java.util.List<Post> getRecentPostsByUser(User user) {
        return postRepository.findTop5ByAuthorOrderByCreatedAtDesc(user);
    }

    public int countPostsByUser(User user) {
        return postRepository.countByAuthor(user);
    }

    public record ReactionResult(String reactionType, boolean active, int likeCount, int disappointedCount) {}

    private int normalizePage(int page) {
        return Math.max(0, page);
    }

    private String validateCategory(String category) {
        if (category == null || !VALID_CATEGORIES.contains(category)) {
            throw new IllegalArgumentException("Invalid post category.");
        }
        return category;
    }

    private String requireText(String value, String fieldName, int maxLength) {
        String trimmed = value == null ? "" : value.trim();
        if (trimmed.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required.");
        }
        if (trimmed.length() > maxLength) {
            throw new IllegalArgumentException(fieldName + " is too long.");
        }
        return trimmed;
    }

    private String validateReactionType(String reactionType) {
        if (!PostLike.TYPE_LIKE.equals(reactionType) && !PostLike.TYPE_DISAPPOINTED.equals(reactionType)) {
            throw new IllegalArgumentException("Invalid reaction type.");
        }
        return reactionType;
    }

    private void incrementReactionCount(Post post, String reactionType) {
        if (PostLike.TYPE_LIKE.equals(reactionType)) {
            post.setLikeCount(post.getLikeCount() + 1);
        } else if (PostLike.TYPE_DISAPPOINTED.equals(reactionType)) {
            post.setDisappointedCount(post.getDisappointedCount() + 1);
        }
    }

    private void decrementReactionCount(Post post, String reactionType) {
        if (PostLike.TYPE_LIKE.equals(reactionType)) {
            post.setLikeCount(Math.max(0, post.getLikeCount() - 1));
        } else if (PostLike.TYPE_DISAPPOINTED.equals(reactionType)) {
            post.setDisappointedCount(Math.max(0, post.getDisappointedCount() - 1));
        }
    }
}
