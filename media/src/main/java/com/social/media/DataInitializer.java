package com.social.media;

import com.social.media.models.Post;
import com.social.media.models.SocialGroup;
import com.social.media.models.SocialProfile;
import com.social.media.models.SocialUser;
import com.social.media.repositories.PostRepository;
import com.social.media.repositories.SocialGroupRepository;
import com.social.media.repositories.SocialProfileRepository;
import com.social.media.repositories.SocialUserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

// @Configuration
// → このクラスをSpringの「設定クラス」として登録する。
// → Spring Boot起動時に、このクラスの中にある
//    @Beanが付いたメソッドを確認し、
//    その戻り値をSpringのBeanとして登録する。
//
// 今回は、下の initializeDate() メソッドに
// @Beanが付いているため、
// CommandLineRunnerがSpringのBeanとして登録される。
//
// その結果、アプリケーション起動時に
// CommandLineRunnerの処理が実行され、
// SocialUser、SocialGroup、Post、SocialProfileなどの
// 初期データをDBへ登録できる。
@Configuration
public class DataInitializer {

    private final SocialUserRepository userRepository;
    private final SocialGroupRepository groupRepository;
    private final SocialProfileRepository socialProfileRepository;
    private final PostRepository postRepository;

    public DataInitializer(SocialUserRepository socialUserRepository,
                           SocialGroupRepository socialGroupRepository,
                           SocialProfileRepository socialProfileRepository,
                           PostRepository postRepository) {
        this.userRepository = socialUserRepository;
        this.groupRepository = socialGroupRepository;
        this.socialProfileRepository = socialProfileRepository;
        this.postRepository = postRepository;
    }

    // @Bean
    // → このメソッドの戻り値をSpring IoCコンテナに
    //    「Bean」として登録することを指定する。
    //
    // → 今回のinitializeDate()メソッドの戻り値は
    //    CommandLineRunnerオブジェクトなので、
    //    CommandLineRunnerがSpringの管理対象になる。
    //
    // → Spring Bootはアプリケーション起動時に
    //    CommandLineRunnerを実行する。
    @Bean
    // CommandLineRunner は、
    //Spring Bootアプリケーションの起動が完了した後に、特定の処理を1回実行する
    //ためのインターフェースです。
    public CommandLineRunner initializeDate() {
        // args -> がラムダ式
        return args -> {
            SocialUser user1 = new SocialUser();
            SocialUser user2 = new SocialUser();
            SocialUser user3 = new SocialUser();

            userRepository.save(user1);
            userRepository.save(user2);
            userRepository.save(user3);

            // Create some groups
            SocialGroup group1 = new SocialGroup();
            SocialGroup group2 = new SocialGroup();

            // Add users to groups
            group1.getSocialUsers().add(user1);
            group1.getSocialUsers().add(user2);

            group2.getSocialUsers().add(user2);
            group2.getSocialUsers().add(user3);

            // Save groups to the repository
            groupRepository.save(group1);
            groupRepository.save(group2);

            // Associate users with groups
            user1.getGroups().add(group1);
            user2.getGroups().add(group1);
            user2.getGroups().add(group2);
            user3.getGroups().add(group2);

            // Save users back to database to update the relationship
            userRepository.save(user1);
            userRepository.save(user2);
            userRepository.save(user3);

            // Create some posts
            Post post1 = new Post();
            Post post2 = new Post();
            Post post3 = new Post();

            // Associate posts with users
            post1.setSocialUser(user1);
            post2.setSocialUser(user2);
            post3.setSocialUser(user3);

            // Save posts to the repository
            postRepository.save(post1);
            postRepository.save(post2);
            postRepository.save(post3);

            // Create social profiles for users
            SocialProfile profile1 = new SocialProfile();
            SocialProfile profile2 = new SocialProfile();
            SocialProfile profile3 = new SocialProfile();

            // Associate profiles with users
            profile1.setUser(user1);
            profile2.setUser(user2);
            profile3.setUser(user3);

            // Save profiles to the repository
            socialProfileRepository.save(profile1);
            socialProfileRepository.save(profile2);
            socialProfileRepository.save(profile3);

            // FETECh Types
            System.out.println("Fetching social user with ID 1:");
            userRepository.findById(1L);

        };
    }
}
