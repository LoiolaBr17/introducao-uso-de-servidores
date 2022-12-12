package com.apsmobile.controledeprodutos.activity;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.ImageDecoder;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.apsmobile.controledeprodutos.helper.FirebaseHelper;
import com.apsmobile.controledeprodutos.model.Produto;
import com.apsmobile.controledeprodutos.R;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.gun0912.tedpermission.PermissionListener;
import com.gun0912.tedpermission.TedPermission;
import com.squareup.picasso.Picasso;

import java.io.IOException;
import java.util.List;

public class FormProdutoActivity extends AppCompatActivity {

    private static final int REQUEST_GALERA = 100;

    private ImageView imagem_produto;
    private String caminhoImagem;
    private Bitmap imagem;

    private EditText edit_produto;
    private EditText edit_quantidade;
    private EditText edit_valor;

    private Produto produto;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_form_produto);

        iniciaComponentes();

        configCliques();

        Bundle bundle = getIntent().getExtras();
        if (bundle != null) {
            produto = (Produto) bundle.getSerializable("produto");

            editProduto();
        }

    }

    private void configCliques() {
        findViewById(R.id.ib_voltar).setOnClickListener(view -> finish());
    }

    public void verificaPermissaoGalera(View view) {
        PermissionListener permissionListener = new PermissionListener() {
            @Override
            public void onPermissionGranted() {
                abrirGaleria();
            }

            @Override
            public void onPermissionDenied(List<String> deniedPermissions) {
                Toast.makeText(FormProdutoActivity.this, "Permissão Negada.", Toast.LENGTH_SHORT).show();
            }
        };

        showDialogPermissao(permissionListener, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE});

    }

    private void abrirGaleria() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, REQUEST_GALERA);
    }

    private void showDialogPermissao(PermissionListener listener, String[] permissoes) {
        TedPermission.with(this)
                .setPermissionListener(listener)
                .setDeniedTitle("Permissões")
                .setDeniedMessage("Você negou a permissão para acessar a galeria do dispositivo, deseja permitir ?")
                .setDeniedCloseButtonText("Não")
                .setGotoSettingButtonText("Sim")
                .setPermissions(permissoes)
                .check();
    }

    private void editProduto() {
        edit_produto.setText(produto.getNome());
        edit_quantidade.setText(String.valueOf(produto.getEstoque()));
        edit_valor.setText(String.valueOf(produto.getValor()));
        Picasso.get().load(produto.getUrlImagem()).into(imagem_produto);
    }

    public void salvarProduto(View view) {

        String nome = edit_produto.getText().toString();
        String quantidade = edit_quantidade.getText().toString();
        String valor = edit_valor.getText().toString();

        if (!nome.isEmpty()) {
            if (!quantidade.isEmpty()) {

                int qtd = Integer.parseInt(quantidade);

                if (qtd >= 1) {

                    if (!valor.isEmpty()) {

                        double valorProduto = Double.parseDouble(valor);

                        if (valorProduto > 0) {

                            if (produto == null) produto = new Produto();
                            produto.setNome(nome);
                            produto.setEstoque(qtd);
                            produto.setValor(valorProduto);

                            if (caminhoImagem == null) {
                                Toast.makeText(this, "Selecione uma imagem.", Toast.LENGTH_SHORT).show();
                            } else {
                                salvarImagemProduto();
                            }

                            finish();

                        } else {
                            edit_valor.requestFocus();
                            edit_valor.setError("Informe um valor maior que 0.");
                        }

                    } else {
                        edit_valor.requestFocus();
                        edit_valor.setError("Informe o valor do produto.");
                    }

                } else {
                    edit_quantidade.requestFocus();
                    edit_quantidade.setError("Informe um valor maior que 0.");
                }

            } else {
                edit_quantidade.requestFocus();
                edit_quantidade.setError("Informe um valor válido.");
            }
        } else {
            edit_produto.requestFocus();
            edit_produto.setError("Informe o nome do produto.");
        }

    }

    private void salvarImagemProduto() {
        StorageReference reference = FirebaseHelper.getStorageReference()
                .child("imagens")
                .child("produtos")
                .child(FirebaseHelper.getIdFirebase())
                .child(produto.getId() + ".jpeg");

        UploadTask uploadTask = reference.putFile(Uri.parse(caminhoImagem));
        uploadTask.addOnSuccessListener(taskSnapshot -> reference.getDownloadUrl().addOnCompleteListener(task -> {

            produto.setUrlImagem(task.getResult().toString());
            produto.salvarProduto();

            finish();

        })).addOnFailureListener(e -> Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void iniciaComponentes() {
        edit_produto = findViewById(R.id.edit_produto);
        edit_quantidade = findViewById(R.id.edit_quantidade);
        edit_valor = findViewById(R.id.edit_valor);
        imagem_produto = findViewById(R.id.imagem_produto);

        TextView text_titulo = findViewById(R.id.text_titulo);
        text_titulo.setText("Form produto");
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK) {
            if (requestCode == REQUEST_GALERA) {

                Uri localImagemSelecionada = data.getData();
                caminhoImagem = localImagemSelecionada.toString();

                if (Build.VERSION.SDK_INT < 28) {
                    try {
                        imagem = MediaStore.Images.Media.getBitmap(getBaseContext().getContentResolver(), localImagemSelecionada);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                } else {
                    ImageDecoder.Source source = ImageDecoder.createSource(getBaseContext().getContentResolver(), localImagemSelecionada);
                    try {
                        imagem = ImageDecoder.decodeBitmap(source);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                imagem_produto.setImageBitmap(imagem);
            }
        }

    }

}