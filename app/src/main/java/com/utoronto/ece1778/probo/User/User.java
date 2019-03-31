package com.utoronto.ece1778.probo.User;

import android.graphics.Bitmap;
import android.support.annotation.NonNull;
import android.util.Log;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthInvalidUserException;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseAuthWeakPasswordException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.utoronto.ece1778.probo.News.Annotation;
import com.utoronto.ece1778.probo.News.AnnotationVote;
import com.utoronto.ece1778.probo.News.Article;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class User {
    public static final int
            SIGN_IN_ERROR_EMPTY_EMAIL = 0,
            SIGN_IN_ERROR_EMPTY_PASSWORD = 1,
            SIGN_IN_ERROR_INVALID_CREDENTIALS = 2,
            SIGN_IN_ERROR_NO_USER = 3;

    public static final int
            SIGN_UP_ERROR_EMPTY_EMAIL = 0,
            SIGN_UP_ERROR_EMPTY_PASSWORD = 1,
            SIGN_UP_ERROR_INCORRECT_RE_PASSWORD = 2,
            SIGN_UP_ERROR_EMPTY_NAME = 3,
            SIGN_UP_ERROR_WEAK_PASSWORD = 4,
            SIGN_UP_ERROR_INVALID_EMAIL = 5,
            SIGN_UP_ERROR_USER_EXISTS = 6;

    public static final int
            SIGN_UP_PROGRESS_CREATING = 0,
            SIGN_UP_PROGRESS_UPLOADING_PROFILE_IMAGE = 1,
            SIGN_UP_PROGRESS_SAVING = 2;

    public static final int
            UPDATE_ERROR_EMPTY_NAME = 0;

    public static final int
            UPDATE_PROGRESS_UPLOADING_PROFILE_IMAGE = 0,
            UPDATE_PROGRESS_SAVING = 1;

    private String uid, profileImagePath, name, title;
    private ArrayList<Annotation> annotations;
    private ArrayList<Subscription> subscriptions;
    private ArrayList<User> following;
    private ArrayList<User> followers;
    private ArrayList<Annotation> notifications;

    private boolean loggedInUser;
    private boolean loaded;

    public User() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        if (user != null) {
            this.uid = user.getUid();
        }

        this.annotations = new ArrayList<>();
        this.subscriptions = new ArrayList<>();
        this.following = new ArrayList<>();
        this.followers = new ArrayList<>();
        this.notifications = new ArrayList<>();

        this.loggedInUser = true;
        this.loaded = false;
    }

    public User(String uid) {
        this.uid = uid;

        this.loggedInUser = false;
        this.loaded = false;
    }

    public String getUid() {
        return this.uid;
    }

    public String getName() {
        return this.name;
    }

    public String getTitle() {
        return this.title;
    }

    public String getProfileImagePath() {
        return this.profileImagePath;
    }

    public ArrayList<Annotation> getAnnotations() {
        return this.annotations;
    }

    public ArrayList<Subscription> getSubscriptions() {
        return this.subscriptions;
    }

    public ArrayList<User> getFollowing() {
        return this.following;
    }

    public ArrayList<User> getFollowers() {
        return this.followers;
    }

    public ArrayList<Annotation> getNotifications() {
        return this.notifications;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setProfileImagePath(String profileImagePath) {
        this.profileImagePath = profileImagePath;
    }

    public boolean isFollowing(User checkUser) {
        return this.following.contains(checkUser);
    }

    public boolean hasLoaded() {
        return this.loaded;
    }

    public void load(final UserCallback cb) {
        if (this.uid == null) {
            cb.onError(new Exception("No user uid was supplied and no user is currently logged in."));
            return;
        }

        FirebaseFirestore db = FirebaseFirestore.getInstance();

        this.loaded = false;

        db.collection("users")
                .document(this.uid)
                .get()
                .addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                    @Override
                    public void onSuccess(DocumentSnapshot documentSnapshot) {
                        profileImagePath = documentSnapshot.getString("profileImagePath");
                        name = documentSnapshot.getString("name");
                        title = documentSnapshot.getString("title");

                        loadFollowing(cb);
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        cb.onError(e);
                    }
                });
    }

    private void loadFollowing(final UserCallback cb) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        this.following.clear();

        db.collection("users")
                .document(this.uid)
                .collection("following")
                .get()
                .addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                    @Override
                    public void onSuccess(QuerySnapshot queryDocumentSnapshots) {
                        for (DocumentSnapshot documentSnapshot : queryDocumentSnapshots) {
                            User followingUser = new User(documentSnapshot.getString("userId"));
                            Subscription subscription = new Subscription(followingUser);

                            following.add(followingUser);
                            createSubscription(subscription);
                        }

                        loadFollowers(cb);
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        cb.onError(e);
                    }
                });
    }

    private void loadFollowers(final UserCallback cb) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        this.followers.clear();

        db.collection("users")
                .document(this.uid)
                .collection("followers")
                .get()
                .addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                    @Override
                    public void onSuccess(QuerySnapshot queryDocumentSnapshots) {
                        for (DocumentSnapshot documentSnapshot : queryDocumentSnapshots) {
                            User followersUser = new User(documentSnapshot.getString("userId"));
                            followers.add(followersUser);
                        }

                        loadSubscriptions(cb);
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        cb.onError(e);
                    }
                });
    }

    public void loadSubscriptions(final UserCallback cb) {
        final FirebaseFirestore db = FirebaseFirestore.getInstance();

        final User currentUser = this;
        this.subscriptions = new ArrayList<>();

        db.collection("users")
                .document(this.uid)
                .collection("subscriptions")
                .get()
                .addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                    @Override
                    public void onSuccess(QuerySnapshot queryDocumentSnapshots) {
                        for (DocumentSnapshot documentSnapshot : queryDocumentSnapshots) {
                            Long startIndex = documentSnapshot.getLong("startIndex");
                            Long endIndex = documentSnapshot.getLong("endIndex");

                            Annotation annotation = new Annotation(
                                    documentSnapshot.getString("annotationId"),
                                    new Article(documentSnapshot.getString("articleId")),
                                    currentUser,
                                    documentSnapshot.getString("type"),
                                    startIndex.intValue(),
                                    endIndex.intValue(),
                                    0,
                                    null,
                                    null,
                                    new HashMap<String, AnnotationVote>(),
                                    new HashMap<String, AnnotationVote>(),
                                    documentSnapshot.getString("heading"),
                                    documentSnapshot.getString("sentence")

                            );

                            Subscription subscription = new Subscription(
                                    annotation,
                                    documentSnapshot.getDate("date")
                            );

                            subscriptions.add(subscription);
                        }

                        createSubscriptions();

                        loaded = true;

                        cb.onLoad();
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        cb.onError(e);
                    }
                });
    }

    public void loadAnnotations(final UserAnnotationsCallback cb) {
        final User currentUser = this;
        final FirebaseFirestore db = FirebaseFirestore.getInstance();

        this.annotations = new ArrayList<>();

        db.collection("annotations")
                .whereEqualTo("userId", this.uid)
                .get()
                .addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                    @Override
                    public void onSuccess(final QuerySnapshot queryDocumentSnapshots) {
                        db.collection("annotation_votes")
                                .get()
                                .addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                                    @Override
                                    public void onSuccess(QuerySnapshot queryVotesDocumentSnapshots) {
                                        for (DocumentSnapshot documentSnapshot : queryDocumentSnapshots) {
                                            String annotationId = documentSnapshot.getId();
                                            Article article = new Article(documentSnapshot.getString("articleId"));
                                            String type = documentSnapshot.getString("type");
                                            Long startIndex = documentSnapshot.getLong("startIndex");
                                            Long endIndex = documentSnapshot.getLong("endIndex");
                                            Long value = documentSnapshot.getLong("value");
                                            HashMap<String, AnnotationVote> upvotes = new HashMap<>();
                                            HashMap<String, AnnotationVote> downvotes = new HashMap<>();
                                            String sentence = documentSnapshot.getString("sentence");

                                            for (DocumentSnapshot votesDocumentSnapshot : queryVotesDocumentSnapshots) {
                                                if (votesDocumentSnapshot.getString("annotationId").equals(annotationId)) {
                                                    AnnotationVote vote = new AnnotationVote(
                                                            votesDocumentSnapshot.getId(),
                                                            new User(votesDocumentSnapshot.getString("userId")),
                                                            votesDocumentSnapshot.getBoolean("value")
                                                    );

                                                    if (vote.getValue()) {
                                                        upvotes.put(
                                                                votesDocumentSnapshot.getString("userId"),
                                                                vote
                                                        );
                                                    } else {
                                                        downvotes.put(
                                                                votesDocumentSnapshot.getString("userId"),
                                                                vote
                                                        );
                                                    }
                                                }
                                            }

                                            Annotation annotation = new Annotation(
                                                    annotationId,
                                                    article,
                                                    currentUser,
                                                    type,
                                                    startIndex.intValue(),
                                                    endIndex.intValue(),
                                                    value.intValue(),
                                                    documentSnapshot.getString("comment"),
                                                    documentSnapshot.getString("source"),
                                                    upvotes,
                                                    downvotes,
                                                    documentSnapshot.getString("heading"),
                                                    documentSnapshot.getString("sentence")
                                            );

                                            annotations.add(annotation);
                                        }

                                        cb.onLoad();
                                    }
                                })
                                .addOnFailureListener(new OnFailureListener() {
                                    @Override
                                    public void onFailure(@NonNull Exception e) {
                                        cb.onError(e);
                                    }
                                });
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        cb.onError(e);
                    }
                });
    }

    public void updateAnnotation(Annotation newAnnotation) {
        int index = 0;
        for (Annotation annotation : this.annotations) {
            if (annotation.getId().equals(newAnnotation.getId())) {
                this.annotations.set(index, newAnnotation);
            }

            index++;
        }
    }

    public static boolean isSignedIn() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        return user != null;
    }

    public static void signIn(final UserSignInCallback cb, String email, String password) {
        if (email == null || email.length() == 0) {
            cb.onSignInError(User.SIGN_IN_ERROR_EMPTY_EMAIL);
            return;
        }

        if (password == null || password.length() == 0) {
            cb.onSignInError(User.SIGN_IN_ERROR_EMPTY_PASSWORD);
            return;
        }

        FirebaseAuth mAuth = FirebaseAuth.getInstance();

        mAuth.signInWithEmailAndPassword(email, password)
                .addOnSuccessListener(new OnSuccessListener<AuthResult>() {
                    @Override
                    public void onSuccess(AuthResult authResult) {
                        final User user = new User();

                        UserCallback userCb = new UserCallback() {
                            @Override
                            public void onLoad() {
                                cb.onSignedIn(user);
                            }

                            @Override
                            public void onError(Exception error) {
                                cb.onError(error);
                            }
                        };

                        user.load(userCb);
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        try {
                            throw e;
                        } catch (FirebaseAuthInvalidCredentialsException err) {
                            cb.onSignInError(User.SIGN_IN_ERROR_INVALID_CREDENTIALS);
                        } catch (FirebaseAuthInvalidUserException err) {
                            cb.onSignInError(User.SIGN_IN_ERROR_NO_USER);
                        } catch (Exception err) {
                            cb.onError(err);
                        }
                    }
                });
    }

    public static void signUp(final UserSignUpCallback cb, final Bitmap profileImage, String email, String password, String rePassword, final String name) {
        if (email == null || email.length() == 0) {
            cb.onSignUpError(User.SIGN_UP_ERROR_EMPTY_EMAIL);
            return;
        }

        if (password == null || password.length() == 0) {
            cb.onSignUpError(User.SIGN_UP_ERROR_EMPTY_PASSWORD);
            return;
        }

        if (!rePassword.equals(password)) {
            cb.onSignUpError(User.SIGN_UP_ERROR_INCORRECT_RE_PASSWORD);
            return;
        }

        if (name == null || name.length() == 0) {
            cb.onSignUpError(User.SIGN_UP_ERROR_EMPTY_NAME);
            return;
        }

        FirebaseAuth mAuth = FirebaseAuth.getInstance();

        cb.onProgress(SIGN_UP_PROGRESS_CREATING);

        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnSuccessListener(new OnSuccessListener<AuthResult>() {
                    @Override
                    public void onSuccess(final AuthResult authResult) {
                        UserUploadProfileImageCallback uploadCb = new UserUploadProfileImageCallback() {
                            @Override
                            public void onUploaded(String path) {
                                Map<String, Object> newUser = new HashMap<>();
                                FirebaseFirestore db = FirebaseFirestore.getInstance();

                                newUser.put("profileImagePath", path);
                                newUser.put("name", name);

                                cb.onProgress(SIGN_UP_PROGRESS_SAVING);

                                db.collection("users")
                                        .document(authResult.getUser().getUid())
                                        .set(newUser)
                                        .addOnSuccessListener(new OnSuccessListener<Void>() {
                                            @Override
                                            public void onSuccess(Void aVoid) {
                                                final User user = new User();

                                                UserCallback userCb = new UserCallback() {
                                                    @Override
                                                    public void onLoad() {
                                                        cb.onSignedUp(user);
                                                    }

                                                    @Override
                                                    public void onError(Exception error) {
                                                        cb.onError(error);
                                                    }
                                                };

                                                user.load(userCb);
                                            }
                                        })
                                        .addOnFailureListener(new OnFailureListener() {
                                            @Override
                                            public void onFailure(@NonNull Exception e) {
                                                cb.onError(e);
                                            }
                                        });
                            }

                            @Override
                            public void onError(Exception error) {
                                cb.onError(error);
                            }
                        };

                        if (profileImage != null) {
                            cb.onProgress(SIGN_UP_PROGRESS_UPLOADING_PROFILE_IMAGE);
                        }

                        User.uploadProfileImage(uploadCb, authResult.getUser().getUid(), profileImage);
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        try {
                            throw e;
                        } catch (FirebaseAuthWeakPasswordException err) {
                            cb.onSignUpError(User.SIGN_UP_ERROR_WEAK_PASSWORD);
                        } catch (FirebaseAuthInvalidCredentialsException err) {
                            cb.onSignUpError(User.SIGN_UP_ERROR_INVALID_EMAIL);
                        } catch (FirebaseAuthUserCollisionException err) {
                            cb.onSignUpError(User.SIGN_UP_ERROR_USER_EXISTS);
                        } catch (Exception err) {
                            cb.onError(err);
                        }
                    }
                });
    }

    public void update(final UserUpdateCallback cb, Bitmap profileImage, final String name, final String title, final boolean profileImageChanged) {
        if (name == null || name.length() == 0) {
            cb.onUpdateError(User.UPDATE_ERROR_EMPTY_NAME);
            return;
        }

        final User currentUser = this;

        UserUploadProfileImageCallback uploadCb = new UserUploadProfileImageCallback() {
            @Override
            public void onUploaded(final String path) {
                Map<String, Object> updatedUser = new HashMap<>();
                FirebaseFirestore db = FirebaseFirestore.getInstance();

                final String newProfileImagePath = profileImageChanged ? path : profileImagePath;

                updatedUser.put("profileImagePath", newProfileImagePath);
                updatedUser.put("name", name);
                updatedUser.put("title", title);

                cb.onProgress(User.UPDATE_PROGRESS_SAVING);

                db.collection("users")
                        .document(uid)
                        .update(updatedUser)
                        .addOnSuccessListener(new OnSuccessListener<Void>() {
                            @Override
                            public void onSuccess(Void aVoid) {
                                currentUser.setProfileImagePath(newProfileImagePath);
                                currentUser.setName(name);
                                currentUser.setTitle(title);

                                cb.onUpdate(currentUser);
                            }
                        })
                        .addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                cb.onError(e);
                            }
                        });
            }

            @Override
            public void onError(Exception error) {
                cb.onError(error);
            }
        };

        if (profileImageChanged && profileImage != null) {
            cb.onProgress(User.UPDATE_PROGRESS_UPLOADING_PROFILE_IMAGE);
        }

        User.uploadProfileImage(uploadCb, this.uid, profileImage);
    }

    private void createSubscriptions() {
        for (Subscription subscription : this.subscriptions) {
            createSubscription(subscription);
        }
    }

    private void createSubscription(Subscription subscription) {
        if (!this.loggedInUser) {
            return;
        }

        FirebaseMessaging firebaseMessaging = FirebaseMessaging.getInstance();
        firebaseMessaging.subscribeToTopic(subscription.getTopic());
    }

    private void removeSubscription(Subscription subscription) {
        if (!this.loggedInUser) {
            return;
        }

        FirebaseMessaging firebaseMessaging = FirebaseMessaging.getInstance();
        firebaseMessaging.unsubscribeFromTopic(subscription.getTopic());
    }

    public static void uploadProfileImage(final UserUploadProfileImageCallback cb, String userUid, Bitmap image) {
        if (image == null) {
            cb.onUploaded(null);
            return;
        }

        final Long currentTime = System.currentTimeMillis() / 1000;
        final String path = userUid + "/" + currentTime.toString() + ".jpg";

        FirebaseStorage storage = FirebaseStorage.getInstance();
        StorageReference storageRef = storage.getReference();
        StorageReference profileImageRef = storageRef.child(path);

        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        image.compress(Bitmap.CompressFormat.JPEG, 100, byteArrayOutputStream);
        byte[] data = byteArrayOutputStream.toByteArray();

        UploadTask uploadTask = profileImageRef.putBytes(data);

        uploadTask.addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                cb.onUploaded(path);
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                cb.onError(e);
            }
        });
    }

    public void follow(UserFollowCallback cb, User userToFollow) {
        if (this.equals(userToFollow)) {
            cb.onUpdate();
            return;
        }

        if (!this.following.contains(userToFollow)) {
            this.following.add(userToFollow);
            this.addFollowing(cb, userToFollow);
        }
    }

    public void unfollow(UserFollowCallback cb, User userToUnfollow) {
        if (this.following.contains(userToUnfollow)) {
            this.following.remove(userToUnfollow);
            this.removeFollowing(cb, userToUnfollow);
        }
    }

    private void addFollowing(final UserFollowCallback cb, final User userToAdd) {
        final FirebaseFirestore db = FirebaseFirestore.getInstance();
        Map<String, Object> data = new HashMap<>();
        final Date now = new Date();

        final Subscription subscription = new Subscription(userToAdd);

        data.put("userId", userToAdd.getUid());
        data.put("date", now);

        db.collection("users")
                .document(this.uid)
                .collection("following")
                .document(userToAdd.getUid())
                .set(data)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        Map<String, Object> otherUserData = new HashMap<>();

                        otherUserData.put("userId", userToAdd.getUid());
                        otherUserData.put("date", now);

                        db.collection("users")
                                .document(userToAdd.getUid())
                                .collection("followers")
                                .document(uid)
                                .set(otherUserData)
                                .addOnSuccessListener(new OnSuccessListener<Void>() {
                                    @Override
                                    public void onSuccess(Void aVoid) {
                                        createSubscription(subscription);
                                        cb.onUpdate();
                                    }
                                })
                                .addOnFailureListener(new OnFailureListener() {
                                    @Override
                                    public void onFailure(@NonNull Exception e) {
                                        cb.onError(e);
                                    }
                                });
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        cb.onError(e);
                    }
                });
    }

    private void removeFollowing(final UserFollowCallback cb, final User userToRemove) {
        final FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection("users")
                .document(this.uid)
                .collection("following")
                .document(userToRemove.getUid())
                .delete()
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        db.collection("users")
                                .document(userToRemove.getUid())
                                .collection("followers")
                                .document(uid)
                                .delete()
                                .addOnSuccessListener(new OnSuccessListener<Void>() {
                                    @Override
                                    public void onSuccess(Void aVoid) {
                                        cb.onUpdate();
                                    }
                                })
                                .addOnFailureListener(new OnFailureListener() {
                                    @Override
                                    public void onFailure(@NonNull Exception e) {
                                        cb.onError(e);
                                    }
                                });
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        cb.onError(e);
                    }
                });
    }

    public void subscribe(final UserSubscribeCallback cb, Annotation annotation) {
        final Subscription subscription = new Subscription(
                annotation,
                new Date()
        );

        if (this.subscriptions.contains(subscription)) {
            cb.onUpdate();
            return;
        }

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        Map<String, Object> data = new HashMap<>();

        data.put("annotationId", subscription.getAnnotation().getId());
        data.put("articleId", subscription.getAnnotation().getArticle().getId());
        data.put("type", subscription.getAnnotation().getType());
        data.put("startIndex", subscription.getAnnotation().getStartIndex());
        data.put("endIndex", subscription.getAnnotation().getEndIndex());
        data.put("date", subscription.getDate());

        db.collection("users")
                .document(this.uid)
                .collection("subscriptions")
                .document(subscription.getTopic())
                .set(data)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        subscriptions.add(subscription);
                        createSubscription(subscription);

                        cb.onUpdate();
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        cb.onError(e);
                    }
                });
    }

    public void unsubscribe(final UserSubscribeCallback cb, final Subscription subscription) {
        if (!this.subscriptions.contains(subscription)) {
            cb.onUpdate();
            return;
        }

        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection("users")
                .document(this.uid)
                .collection("subscriptions")
                .document(subscription.getTopic())
                .delete()
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        subscriptions.remove(subscription);
                        removeSubscription(subscription);

                        cb.onUpdate();
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        cb.onError(e);
                    }
                });
    }

    public void loadNotifications(final UserNotificationsCallback cb) {
        final FirebaseFirestore db = FirebaseFirestore.getInstance();

        this.notifications.clear();

        db.collection("users")
                .document(this.uid)
                .collection("notifications")
                .get()
                .addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                    @Override
                    public void onSuccess(final QuerySnapshot notificationsDocumentSnapshots) {
                        db.collection("annotation_votes")
                                .get()
                                .addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                                    @Override
                                    public void onSuccess(QuerySnapshot votesDocumentSnapshots) {
                                        for (DocumentSnapshot notificationsDocumentSnapshot : notificationsDocumentSnapshots) {
                                            String annotationId = notificationsDocumentSnapshot.getId();
                                            Article article = new Article(notificationsDocumentSnapshot.getString("articleId"));
                                            String type = notificationsDocumentSnapshot.getString("type");
                                            Long startIndex = notificationsDocumentSnapshot.getLong("startIndex");
                                            Long endIndex = notificationsDocumentSnapshot.getLong("endIndex");
                                            Long value = notificationsDocumentSnapshot.getLong("value");
                                            HashMap<String, AnnotationVote> upvotes = new HashMap<>();
                                            HashMap<String, AnnotationVote> downvotes = new HashMap<>();
                                            String sentence = notificationsDocumentSnapshot.getString("sentence");

                                            for (DocumentSnapshot votesDocumentSnapshot : votesDocumentSnapshots) {
                                                if (votesDocumentSnapshot.getString("annotationId").equals(annotationId)) {
                                                    AnnotationVote vote = new AnnotationVote(
                                                            votesDocumentSnapshot.getId(),
                                                            new User(votesDocumentSnapshot.getString("userId")),
                                                            votesDocumentSnapshot.getBoolean("value")
                                                    );

                                                    if (vote.getValue()) {
                                                        upvotes.put(
                                                                votesDocumentSnapshot.getString("userId"),
                                                                vote
                                                        );
                                                    } else {
                                                        downvotes.put(
                                                                votesDocumentSnapshot.getString("userId"),
                                                                vote
                                                        );
                                                    }
                                                }
                                            }

                                            Annotation annotation = new Annotation(
                                                    annotationId,
                                                    article,
                                                    new User(notificationsDocumentSnapshot.getString("userId")),
                                                    type,
                                                    startIndex.intValue(),
                                                    endIndex.intValue(),
                                                    value.intValue(),
                                                    notificationsDocumentSnapshot.getString("comment"),
                                                    notificationsDocumentSnapshot.getString("source"),
                                                    upvotes,
                                                    downvotes,
                                                    notificationsDocumentSnapshot.getString("heading"),
                                                    notificationsDocumentSnapshot.getString("sentence")
                                            );

                                            notifications.add(annotation);
                                        }

                                        cb.onLoad();
                                    }
                                });
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        cb.onError(e);
                    }
                });
    }

    public void notification(final UserNotificationCallback cb, Annotation annotation) {
        if (this.notifications.contains(annotation)) {
            cb.onUpdate();
            return;
        }

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        Map<String, Object> data = new HashMap<>();

        data.put("annotationId", annotation.getId());
        data.put("articleId", annotation.getArticle().getId());
        data.put("userId", annotation.getUser().getUid());
        data.put("type", annotation.getType());
        data.put("startIndex", annotation.getStartIndex());
        data.put("endIndex", annotation.getEndIndex());
        data.put("value", annotation.getValue());
        data.put("comment", annotation.getComment());
        data.put("source", annotation.getSource());

        db.collection("users")
                .document(this.uid)
                .collection("notifications")
                .add(data)
                .addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
                    @Override
                    public void onSuccess(DocumentReference documentReference) {
                        cb.onUpdate();
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        cb.onError(e);
                    }
                });
    }

    public void clearNotifications(final UserClearNotificationsCallback cb) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        final CollectionReference collection = db.collection("users")
                .document(this.uid)
                .collection("notifications");

        collection.get()
                .addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                    @Override
                    public void onSuccess(QuerySnapshot queryDocumentSnapshots) {
                        for (DocumentSnapshot documentSnapshot : queryDocumentSnapshots) {
                            collection
                                    .document(documentSnapshot.getId())
                                    .delete();
                        }

                        notifications.clear();

                        cb.onClear();
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        cb.onError(e);
                    }
                });
    }

    @Override
    public boolean equals(Object object) {
        if (!(object instanceof User)) {
            return false;
        }

        User otherUser = (User) object;

        return this.uid.equals(otherUser.getUid());
    }

    public interface UserCallback {
        void onLoad();
        void onError(Exception error);
    }

    public interface UserAnnotationsCallback {
        void onLoad();
        void onError(Exception error);
    }

    public interface UserSignInCallback {
        void onSignedIn(User user);
        void onSignInError(int errorCode);
        void onError(Exception error);
    }

    public interface UserSignUpCallback {
        void onSignedUp(User user);
        void onSignUpError(int errorCode);
        void onProgress(int progressCode);
        void onError(Exception error);
    }

    public interface UserUpdateCallback {
        void onUpdate(User user);
        void onUpdateError(int errorCode);
        void onProgress(int progressCode);
        void onError(Exception error);
    }

    public interface UserUploadProfileImageCallback {
        void onUploaded(String path);
        void onError(Exception error);
    }

    public interface UserFollowCallback {
        void onUpdate();
        void onError(Exception error);
    }

    public interface UserSubscribeCallback {
        void onUpdate();
        void onError(Exception error);
    }

    public interface UserNotificationsCallback {
        void onLoad();
        void onError(Exception error);
    }

    public interface UserNotificationCallback {
        void onUpdate();
        void onError(Exception error);
    }

    public interface UserClearNotificationsCallback {
        void onClear();
        void onError(Exception error);
    }

    public interface UserFragmentInteractionListener {
        User getUser();
        void updateUser(User updatedUser);
    }
}
