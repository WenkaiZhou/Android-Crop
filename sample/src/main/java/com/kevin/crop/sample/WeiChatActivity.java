package com.kevin.crop.sample;

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import com.kevin.crop.UCrop;
import com.kevin.crop.WeiChatCropActivity;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

/**
 * Created by zhouwk on 2016/3/30 0030.
 */
public class WeiChatActivity extends BaseActivity implements View.OnClickListener {

    private static final int REQUEST_SELECT_PICTURE = 0x01;
    // 剪切后图像文件
    private Uri mDestinationUri;

    private Button cropBtn;
    private ImageView imageIv;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_weichat);
        mDestinationUri = Uri.fromFile(new File(getCacheDir(), "cropImage.jpeg"));
        initView();

        cropBtn.setOnClickListener(this);
    }

    private void initView() {
        cropBtn = (Button) this.findViewById(R.id.button_crop);
        imageIv = (ImageView) this.findViewById(R.id.weixin_act_iv_image);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.button_crop:
                pickFromGallery();
                break;
        }
    }

    /**
     * 选择图片
     */
    private void pickFromGallery() {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        startActivityForResult(Intent.createChooser(intent, "选择图片"), REQUEST_SELECT_PICTURE);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
            if (requestCode == REQUEST_SELECT_PICTURE) {
                final Uri selectedUri = data.getData();
                if (selectedUri != null) {
                    startCropActivity(data.getData());
                } else {
                    Toast.makeText(WeiChatActivity.this, "无法剪切选择图片", Toast.LENGTH_SHORT).show();
                }
            } else if (requestCode == UCrop.REQUEST_CROP) {
                handleCropResult(data);
            }
        }
    }

    /**
     * 开始剪切图片
     * @param uri
     */
    private void startCropActivity(Uri uri) {
        UCrop.of(uri, mDestinationUri)
                .withTargetActivity(WeiChatCropActivity.class)
                .withAspectRatio(1, 1)
//                .withMaxResultSize(500, 500)
                .start(WeiChatActivity.this);

    }

    /**
     * 处理剪切后的返回值
     * @param result
     */
    private void handleCropResult(Intent result) {
        final Uri resultUri = UCrop.getOutput(result);
        if (resultUri != null) {
//            imageIv.setImageURI(resultUri);
            Bitmap bmp;
            try {
                bmp = MediaStore.Images.Media.getBitmap(getContentResolver(), resultUri);
                imageIv.setImageBitmap(bmp);
            } catch (FileNotFoundException e) {
            } catch (IOException e) {
            }
        } else {
            Toast.makeText(WeiChatActivity.this, "无法剪切选择图片", Toast.LENGTH_SHORT).show();
        }
    }
}
