package com.helpinghandsorg.helpinghands;

import android.content.ContentResolver;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.util.Patterns;
import android.view.View;
import android.webkit.MimeTypeMap;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.squareup.picasso.Picasso;

import java.util.Calendar;

import de.hdodenhof.circleimageview.CircleImageView;

public class RegisterActivity extends AppCompatActivity {
    private Volunteer newVolunteer;
    private FirebaseAuth mAuth;
    private DatabaseReference dbRef, volunteerRef;
    private EditText editEmail, editPassword, editTextConfirmPassword, editTextName;
    private RadioButton radioButtonMale, radioButtonFemale;
    private String fullName, email, profilepicUrl, gender;
    private ProgressBar mProgressBar;
    private static final int PICK_IMAGE_REQUEST = 1;
    private CircleImageView mProfilePicture;
    private Task urlTask;
    private AlertDialog dialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);
        mAuth = FirebaseAuth.getInstance();
        FirebaseDatabase firebaseDatabase = FirebaseDatabase.getInstance();
        dbRef = firebaseDatabase.getReference();
        Button loginButton = findViewById(R.id.buttonLogin);
        Button registerButton = findViewById(R.id.buttonSubmitIssue);
        editTextName = findViewById(R.id.editTextName);
        editEmail = findViewById(R.id.editTextEmail);
        editPassword = findViewById(R.id.editTextPassword);
        editTextConfirmPassword = findViewById(R.id.editTextConfirmPassword);
        radioButtonMale = findViewById(R.id.radioButtonMale);
        radioButtonFemale = findViewById(R.id.radioButtonFemale);
        mProgressBar = findViewById(R.id.progressBarRegister);
        mProfilePicture = findViewById(R.id.selectProfilePic);
        profilepicUrl = "null";
        //fetching radio button data
        radioButtonMale.setChecked(true);
        gender = "Male";
        final String[] chooseImageOptionList = getResources().getStringArray(R.array.profile_picture_options);

        mProfilePicture.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new AlertDialog.Builder(RegisterActivity.this)
                        .setTitle("Select Your Choice")
                        .setItems(chooseImageOptionList, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                switch (which) {
                                    case 0:
                                        if (!editEmail.getText().toString().isEmpty() && isUserNameValid(editEmail.getText().toString())) {
                                            openFileChooser();
                                            break;
                                        } else {
                                            new AlertDialog.Builder(RegisterActivity.this).setTitle("Error")
                                                    .setMessage("Please enter a valid email first!")
                                                    .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                                                        @Override
                                                        public void onClick(DialogInterface dialog, int which) {
                                                            dialog.dismiss();
                                                        }
                                                    }).show();
                                            editEmail.setError("Enter a valid email address");
                                            editEmail.requestFocus();
                                            break;
                                        }
                                    case 1:
                                        removeProfilePicture();
                                        break;
                                }
                            }
                        }).show();
            }
        });

        registerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    //Create loading dialog box
                    AlertDialog.Builder builder = new AlertDialog.Builder(RegisterActivity.this);
                    builder.setTitle("Please Wait").setView(R.layout.my_progress_view).setCancelable(false);
                    dialog = builder.show();
                    createUser();
                } catch (Exception e) {

                }
            }
        });
        loginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                loginActivity();
            }
        });
    }

    private void loginActivity() {
        Intent intent = new Intent(this, LoginActivity.class);
        startActivity(intent);
        finishAffinity();
    }

    private void createUser() {
        //getting registration form data
        volunteerRef = dbRef.child("Volunteer").child("Member");
        String password = editPassword.getText().toString().trim();
        String confirmPassword = editTextConfirmPassword.getText().toString().trim();
        fullName = editTextName.getText().toString().trim();
        email = editEmail.getText().toString();
        Calendar calendar = Calendar.getInstance();
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH) + 1;
        int day = calendar.get(Calendar.DAY_OF_MONTH);
        final String joiningDate = day + "/" + month + "/" + year;

        if (radioButtonFemale.isChecked()) {
            gender = "Female";
        }

        //checking if fullname field is empty
        if (TextUtils.isEmpty(fullName)) {
            editTextName.setError("Please enter your name");
            editTextName.requestFocus();
            dialog.dismiss();

        }
        if (TextUtils.isEmpty(email)) {
            editEmail.setError("Please enter you email");
            editEmail.requestFocus();
            dialog.dismiss();

        }

        if (TextUtils.isEmpty(password)) {
            editPassword.setError("Please enter a password");
            editPassword.requestFocus();
            dialog.dismiss();

        }

        if (TextUtils.isEmpty(confirmPassword)) {
            editTextConfirmPassword.setError("This field cannot be empty");
            editTextConfirmPassword.requestFocus();
            dialog.dismiss();

        }

        if (!confirmPassword.equals(password)) {
            Log.d("signup", "confirm pass checked");
            editTextConfirmPassword.setError("Password did not match");
            editTextConfirmPassword.requestFocus();
            dialog.dismiss();

        }

        if (isUserNameValid(email) && isPasswordValid(password)) {
            Log.d("emailerror", String.valueOf(isUserNameValid(email)));
            mAuth = FirebaseAuth.getInstance();
            Log.d("signup", "creating account");
            final String finalGender = gender;
            mAuth.createUserWithEmailAndPassword(email, password)
                    .addOnCompleteListener(RegisterActivity.this, new OnCompleteListener<AuthResult>() {
                        @Override
                        public void onComplete(@NonNull Task<AuthResult> task) {
                            if (task.isSuccessful()) {
                                mAuth.getCurrentUser().sendEmailVerification().addOnCompleteListener(new OnCompleteListener<Void>() {
                                    @Override
                                    public void onComplete(@NonNull Task<Void> task) {
                                        new AlertDialog.Builder(RegisterActivity.this)
                                                .setMessage("A verification email has been sent to " + editEmail.getText().toString())
                                                .setTitle("Verify")
                                                .setIcon(android.R.drawable.ic_dialog_info)
                                                .setPositiveButton("Open Email", new DialogInterface.OnClickListener() {
                                                    @Override
                                                    public void onClick(DialogInterface dialog, int which) {
                                                        RegisterActivity.this.dialog.dismiss();
                                                        Intent gmailIntent = new Intent(Intent.ACTION_MAIN);
                                                        gmailIntent.addCategory(Intent.CATEGORY_APP_EMAIL);
                                                        if (gmailIntent == null) {
                                                            Toast.makeText(RegisterActivity.this, "Gmail App is not installed", Toast.LENGTH_SHORT).show();
                                                        } else {
                                                            startActivity(gmailIntent);
                                                        }
                                                    }
                                                })
                                                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                                                    @Override
                                                    public void onClick(DialogInterface dialog, int which) {
                                                        dialog.dismiss();
                                                        startActivity(new Intent(RegisterActivity.this, RegisterActivity.class));
                                                        finishAffinity();
                                                    }
                                                }).show();
                                    }
                                });
                                //volunteer data
                                newVolunteer = new Volunteer(
                                        fullName,
                                        email,
                                        finalGender,
                                        "false",
                                        joiningDate,
                                        profilepicUrl,
                                        "Member",
                                        FirebaseAuth.getInstance().getUid(),
                                        new Long((long) 0.5),
                                        new Long(0),
                                        new Long(0));
                                volunteerRef.child(FirebaseAuth.getInstance().getCurrentUser().getUid())
                                        .setValue(newVolunteer).addOnCompleteListener(new OnCompleteListener<Void>() {
                                    @Override
                                    public void onComplete(@NonNull final Task<Void> task) {
                                        // Sign in success, update UI with the signed-in user's information
                                        Log.d("signup", "createUserWithEmail:success");
                                        final FirebaseUser user = mAuth.getCurrentUser();
                                        if (profilepicUrl.equals("null")) {
                                            new AlertDialog.Builder(RegisterActivity.this)
                                                    .setTitle("Oops!")
                                                    .setMessage("Looks like you didn't choose any profile picture, " +
                                                            "you can also set it later from your profile. ")
                                                    .setPositiveButton("I'll do it later", new DialogInterface.OnClickListener() {
                                                        @Override
                                                        public void onClick(DialogInterface dialog, int which) {
                                                            updateUI(user, task);
                                                        }
                                                    })
                                                    .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                                                        @Override
                                                        public void onClick(DialogInterface dialog, int which) {
                                                            dialog.dismiss();
                                                        }
                                                    }).show();
                                        }
                                    }
                                });
                            } else {
                                // If sign in fails, display a message to the user.
                                Log.w("signup", "createUserWithEmail:failure", task.getException());
                                dialog.dismiss();
                            }

                        }
                    }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    new AlertDialog.Builder(RegisterActivity.this).setMessage(e.getMessage())
                            .setTitle("Oops")
                            .setIcon(android.R.drawable.ic_dialog_info)
                            .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    editEmail.requestFocus();
                                }
                            }).show();

                    dialog.dismiss();
                }
            });
        } else if (!isUserNameValid(email)) {
            editEmail.setError("Invalid Email");
            editEmail.requestFocus();
            dialog.dismiss();
        } else if (!isPasswordValid(password)) {
            editPassword.setError("Password is too small");
            editPassword.requestFocus();
            dialog.dismiss();
        } else {
            Log.d("signup", "unknown error");
        }
    }

    private void updateUI(FirebaseUser user, Task task) {
        dialog.dismiss();
        if (user == null) {
            Toast.makeText(this, "Error: " + task.getException(), Toast.LENGTH_LONG).show();
        }
        readData(new FirebaseCallBack() {
            @Override
            public void Callback(String adminStatus) {
                if (!(adminStatus == null)) {
                    if (adminStatus.equals("true")) {
                        //Update UI with admin activity
                        Intent intent = new Intent(RegisterActivity.this, Main2Activity.class);
                        startActivity(intent);
                        finishAffinity();
                    } else if (adminStatus.equals("false")) {
                        //Update UI with user activity
                        Intent intent = new Intent(RegisterActivity.this, MainActivity.class);
                        startActivity(intent);
                        finishAffinity();
                    }
                }
            }
        });
    }

    //Read data from firebase
    private void readData(final FirebaseCallBack firebaseCallBack) {
        DatabaseReference volunteerRef = dbRef.child("Volunteer").child("Member");
        String uid = mAuth.getCurrentUser().getUid();
        volunteerRef.child(uid).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                String adminStatus = (String) dataSnapshot.child("admin").getValue();
                firebaseCallBack.Callback(adminStatus);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.d("dberror", databaseError.getMessage());
            }
        });
    }

    //Used to fetch data outside the onDataChange() method and checks if data is downloaded
    private interface FirebaseCallBack {
        void Callback(String adminStatus);
    }

    // A placeholder username validation check
    private boolean isUserNameValid(String username) {
        if (username == null) {
            return false;
        }
        if (username.contains("@")) {
            return Patterns.EMAIL_ADDRESS.matcher(username).matches();
        } else {
            return false;
        }
    }

    // A placeholder password validation check
    private boolean isPasswordValid(String password) {
        return password != null && password.trim().length() > 5;
    }

    //From here to last is the code to pick image from gallery and set to database and get url
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK
                && data != null && data.getData() != null) {
            //Sets the path in storage to "Volunteer/EmailofUser/profile_picture"
            StorageReference mStorageRef = FirebaseStorage.getInstance().getReference("Volunteer")
                    .child(editEmail.getText().toString())
                    .child("profile_picture");

            //picked image from storage and put it into mImageUri
            Uri mImageUri = data.getData();

            //Load Image to imageview avatar
            Picasso.get()
                    .load(mImageUri)
                    .fit()
                    .into(mProfilePicture);

            //puts file name as system_time_in_milliseconds.jpg
            StorageReference fileReference = mStorageRef.child(System.currentTimeMillis() + "." + getFileExtension(mImageUri));

            fileReference.putFile(mImageUri)
                    .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                        @Override
                        public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                            urlTask = taskSnapshot.getMetadata().getReference().getDownloadUrl();
                            urlTask.continueWith(new Continuation() {
                                @Override
                                public Object then(@NonNull Task task) throws Exception {
                                    profilepicUrl = task.getResult().toString();
                                    return null;
                                }
                            });

                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {

                        }
                    });

        }

    }

    private String getFileExtension(Uri uri) {
        ContentResolver contentResolver = getContentResolver();
        MimeTypeMap mimeTypeMap = MimeTypeMap.getSingleton();
        return mimeTypeMap.getExtensionFromMimeType(contentResolver.getType(uri));
    }

    //Removes profile picture from database
    private void removeProfilePicture() {
        FirebaseDatabase.getInstance().getReference().child("Volunteer").child("Member")
                .child(FirebaseAuth.getInstance().getCurrentUser().getUid())
                .child("profilePicUrl")
                .setValue("null");
        if (!editEmail.getText().toString().isEmpty() && isUserNameValid(editEmail.getText().toString())) {
            FirebaseStorage.getInstance().getReference()
                    .child("Volunteer")
                    .child(editEmail.getText().toString())
                    .child("profile_picture")
                    .delete();
        }
    }

    //Choose images from device
    private void openFileChooser() {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(intent, PICK_IMAGE_REQUEST);
    }

}