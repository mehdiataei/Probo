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
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.utoronto.ece1778.probo.News.Annotation;
import com.utoronto.ece1778.probo.News.AnnotationVote;
import com.utoronto.ece1778.probo.News.Article;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
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

    private String uid, profileImagePath, name, title;
    private ArrayList<Annotation> annotations;
    private boolean loaded;

    public User() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        if (user != null) {
            this.uid = user.getUid();
        }

        this.annotations = new ArrayList<>();
        this.loaded = false;
    }

    public User(String uid) {
        this.uid = uid;
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
                                                    upvotes,
                                                    downvotes
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

    public interface UserUploadProfileImageCallback {
        void onUploaded(String path);
        void onError(Exception error);
    }
}
