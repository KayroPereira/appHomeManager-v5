package com.home.apphomemanager_v5.view;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.LinearInterpolator;

import androidx.annotation.Nullable;

import java.util.Random;

/**
 * Reservatório desenhado em Canvas: água com ondas animadas, nível que sobe/desce suavemente,
 * bolhas quando está enchendo e fluxo animado nos canos de entrada (topo) e saída (base).
 * Nível desconhecido (fração negativa) mostra o tanque vazio com "--".
 */
public class TanqueView extends View {

    private static final int QTD_BOLHAS = 9;
    private static final long DURACAO_ONDA_MS = 2600;
    private static final long DURACAO_NIVEL_MS = 900;

    private static final int COR_AGUA = 0xFF29B6F6;
    private static final int COR_ATENCAO = 0xFFFFB74D;
    private static final int COR_CRITICO = 0xFFEF5350;
    private static final int COR_INATIVO = 0xFF78909C;

    private final Paint paintCorpo = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint paintBorda = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint paintTampa = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint paintAgua = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint paintAguaFundo = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint paintProfundidade = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint paintBolha = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint paintCano = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint paintFluxo = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint paintMarca = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint paintTexto = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint paintLegenda = new Paint(Paint.ANTI_ALIAS_FLAG);

    private final RectF corpo = new RectF();
    private final RectF tampa = new RectF();
    private final Path recorte = new Path();
    private final Path onda = new Path();

    private final float dp;
    private final float[] bolhaX = new float[QTD_BOLHAS];
    private final float[] bolhaFase = new float[QTD_BOLHAS];
    private final float[] bolhaRaio = new float[QTD_BOLHAS];
    private final float[] bolhaVel = new float[QTD_BOLHAS];

    private float nivelAlvo = -1f;
    private float nivelExibido = 0f;
    private float faseOnda = 0f;
    private float faseFluxo = 0f;

    private boolean enchendo = false;
    private boolean esvaziando = false;

    private ValueAnimator animadorOnda;
    private ValueAnimator animadorNivel;

    public TanqueView(Context context) {
        this(context, null);
    }

    public TanqueView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public TanqueView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        dp = context.getResources().getDisplayMetrics().density;

        paintCorpo.setStyle(Paint.Style.FILL);
        paintCorpo.setColor(0x1AFFFFFF);

        paintBorda.setStyle(Paint.Style.STROKE);
        paintBorda.setStrokeWidth(2.5f * dp);
        paintBorda.setColor(0x99FFFFFF);

        paintTampa.setStyle(Paint.Style.FILL);
        paintTampa.setColor(0x66FFFFFF);

        paintAgua.setStyle(Paint.Style.FILL);
        paintAguaFundo.setStyle(Paint.Style.FILL);
        paintProfundidade.setStyle(Paint.Style.FILL);

        paintBolha.setStyle(Paint.Style.STROKE);
        paintBolha.setStrokeWidth(1.5f * dp);
        paintBolha.setColor(0xB3FFFFFF);

        paintCano.setStyle(Paint.Style.STROKE);
        paintCano.setStrokeWidth(8f * dp);
        paintCano.setStrokeCap(Paint.Cap.ROUND);
        paintCano.setColor(0x55FFFFFF);

        paintFluxo.setStyle(Paint.Style.FILL);

        paintMarca.setStyle(Paint.Style.STROKE);
        paintMarca.setStrokeWidth(1.5f * dp);
        paintMarca.setStrokeCap(Paint.Cap.ROUND);
        paintMarca.setColor(0x55FFFFFF);

        paintTexto.setColor(Color.WHITE);
        paintTexto.setTextAlign(Paint.Align.CENTER);
        paintTexto.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        paintTexto.setTextSize(40f * dp);
        paintTexto.setShadowLayer(6f * dp, 0, 2f * dp, 0x99000000);

        paintLegenda.setColor(0xCCFFFFFF);
        paintLegenda.setTextAlign(Paint.Align.CENTER);
        paintLegenda.setTextSize(13f * dp);
        paintLegenda.setShadowLayer(4f * dp, 0, 1f * dp, 0x99000000);

        Random random = new Random(7);
        for (int i = 0; i < QTD_BOLHAS; i++) {
            bolhaX[i] = 0.1f + 0.8f * random.nextFloat();
            bolhaFase[i] = random.nextFloat();
            bolhaRaio[i] = (2f + 3.5f * random.nextFloat()) * dp;
            bolhaVel[i] = 0.6f + 0.8f * random.nextFloat();
        }
    }

    /**
     * Define o nível de 0 (vazio) a 1 (cheio); negativo = desconhecido.
     * O desenho acompanha o valor com uma animação suave.
     */
    public void setNivel(float fracao) {

        float novo = fracao < 0 ? -1f : Math.min(1f, fracao);

        if (novo == nivelAlvo) {
            return;
        }

        boolean primeiro = nivelAlvo < 0 && novo >= 0;
        nivelAlvo = novo;

        if (animadorNivel != null) {
            animadorNivel.cancel();
        }

        float destino = Math.max(0f, novo);

        // Sem tamanho ainda (ou primeira leitura) não há o que animar: vai direto ao valor.
        if (!isShown() || getWidth() == 0) {
            nivelExibido = destino;
            invalidate();
            return;
        }

        animadorNivel = ValueAnimator.ofFloat(nivelExibido, destino);
        animadorNivel.setDuration(primeiro ? DURACAO_NIVEL_MS + 400 : DURACAO_NIVEL_MS);
        animadorNivel.setInterpolator(new DecelerateInterpolator(1.6f));
        animadorNivel.addUpdateListener(a -> {
            nivelExibido = (float) a.getAnimatedValue();
            invalidate();
        });
        animadorNivel.start();
    }

    /** Entrada de água aberta: bolhas subindo e gotas descendo pelo cano de cima. */
    public void setEnchendo(boolean enchendo) {

        if (this.enchendo != enchendo) {
            this.enchendo = enchendo;
            invalidate();
        }
    }

    /** Saída de água ativa (bomba): gotas correndo pelo cano de baixo. */
    public void setEsvaziando(boolean esvaziando) {

        if (this.esvaziando != esvaziando) {
            this.esvaziando = esvaziando;
            invalidate();
        }
    }

    @Override
    public void setEnabled(boolean enabled) {

        if (isEnabled() != enabled) {
            super.setEnabled(enabled);
            invalidate();
        }
    }

    @Override
    protected void onAttachedToWindow() {

        super.onAttachedToWindow();
        iniciaOnda();
    }

    @Override
    protected void onDetachedFromWindow() {

        paraAnimacoes();
        super.onDetachedFromWindow();
    }

    @Override
    protected void onWindowVisibilityChanged(int visibility) {

        super.onWindowVisibilityChanged(visibility);

        if (visibility == VISIBLE) {
            iniciaOnda();
        } else {
            paraAnimacoes();
        }
    }

    private void iniciaOnda() {

        if (animadorOnda != null && animadorOnda.isRunning()) {
            return;
        }

        animadorOnda = ValueAnimator.ofFloat(0f, 1f);
        animadorOnda.setDuration(DURACAO_ONDA_MS);
        animadorOnda.setInterpolator(new LinearInterpolator());
        animadorOnda.setRepeatCount(ValueAnimator.INFINITE);
        animadorOnda.addUpdateListener(a -> {
            faseOnda = (float) a.getAnimatedValue();
            faseFluxo = (faseFluxo + 0.012f) % 1f;
            invalidate();
        });
        animadorOnda.start();
    }

    private void paraAnimacoes() {

        if (animadorOnda != null) {
            animadorOnda.cancel();
            animadorOnda = null;
        }
        if (animadorNivel != null) {
            animadorNivel.cancel();
            animadorNivel = null;
            nivelExibido = Math.max(0f, nivelAlvo);
        }
    }

    private int corDaAgua(float nivel) {

        if (!isEnabled() || nivelAlvo < 0) {
            return COR_INATIVO;
        }
        if (nivel < 0.2f) {
            return COR_CRITICO;
        }
        if (nivel < 0.5f) {
            return mistura(COR_ATENCAO, COR_AGUA, (nivel - 0.2f) / 0.3f);
        }
        return COR_AGUA;
    }

    private static int mistura(int de, int para, float t) {

        t = Math.max(0f, Math.min(1f, t));
        int r = (int) (Color.red(de) + (Color.red(para) - Color.red(de)) * t);
        int g = (int) (Color.green(de) + (Color.green(para) - Color.green(de)) * t);
        int b = (int) (Color.blue(de) + (Color.blue(para) - Color.blue(de)) * t);
        return Color.rgb(r, g, b);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {

        super.onSizeChanged(w, h, oldw, oldh);

        float margemX = 30f * dp;
        float topo = 26f * dp;
        float base = 8f * dp;

        corpo.set(margemX, topo, w - margemX, h - base);
        tampa.set(margemX - 8f * dp, topo - 12f * dp, w - margemX + 8f * dp, topo + 4f * dp);

        recorte.reset();
        recorte.addRoundRect(corpo, 22f * dp, 22f * dp, Path.Direction.CW);

        paintProfundidade.setShader(new LinearGradient(0, corpo.top, 0, corpo.bottom,
                0x00000000, 0x66000020, Shader.TileMode.CLAMP));
    }

    @Override
    protected void onDraw(Canvas canvas) {

        super.onDraw(canvas);

        if (corpo.isEmpty()) {
            return;
        }

        float raio = 22f * dp;
        float nivel = nivelExibido;
        boolean ativo = isEnabled();
        int cor = corDaAgua(nivel);

        desenhaCanos(canvas, nivel);

        // Corpo e tampa
        canvas.drawRoundRect(corpo, raio, raio, paintCorpo);
        canvas.drawRoundRect(tampa, 8f * dp, 8f * dp, paintTampa);

        // Água, recortada pelo formato do tanque
        float alturaAgua = corpo.height() * nivel;
        float yAgua = corpo.bottom - alturaAgua;

        if (nivel > 0.002f) {

            canvas.save();
            canvas.clipPath(recorte);

            float amplitude = Math.min(7f * dp, 3f * dp + alturaAgua * 0.05f);
            if (!ativo) {
                amplitude *= 0.35f;
            }

            paintAguaFundo.setColor(cor);
            paintAguaFundo.setAlpha(120);
            desenhaOnda(canvas, yAgua - 4f * dp, amplitude, faseOnda * 2f * (float) Math.PI + 1.7f, 1.3f, paintAguaFundo);

            paintAgua.setColor(cor);
            paintAgua.setAlpha(ativo ? 235 : 190);
            desenhaOnda(canvas, yAgua, amplitude, -faseOnda * 2f * (float) Math.PI, 1f, paintAgua);

            canvas.drawRect(corpo.left, yAgua - amplitude, corpo.right, corpo.bottom, paintProfundidade);

            if (enchendo && ativo) {
                desenhaBolhas(canvas, yAgua);
            }

            canvas.restore();
        }

        // Marcas de nível (25/50/75%)
        for (int i = 1; i <= 3; i++) {
            float y = corpo.bottom - corpo.height() * i * 0.25f;
            float comprimento = (i == 2 ? 16f : 10f) * dp;
            canvas.drawLine(corpo.left + 10f * dp, y, corpo.left + 10f * dp + comprimento, y, paintMarca);
        }

        canvas.drawRoundRect(corpo, raio, raio, paintBorda);

        // Percentual
        float cx = corpo.centerX();
        float cy = corpo.centerY();
        String texto = nivelAlvo < 0 ? "--" : Math.round(nivel * 100) + "%";
        paintTexto.setAlpha(ativo ? 255 : 150);
        canvas.drawText(texto, cx, cy + 14f * dp, paintTexto);

        if (nivelAlvo >= 0) {
            String legenda = nivel < 0.2f ? "Nível baixo" : (nivel > 0.95f ? "Cheio" : "");
            if (!legenda.isEmpty()) {
                canvas.drawText(legenda, cx, cy + 36f * dp, paintLegenda);
            }
        }
    }

    private void desenhaOnda(Canvas canvas, float yBase, float amplitude, float fase, float frequencia, Paint paint) {

        float largura = corpo.width();
        float comprimentoOnda = largura / (1.4f * frequencia);

        onda.reset();
        onda.moveTo(corpo.left, corpo.bottom + 2f * dp);
        onda.lineTo(corpo.left, yBase);

        for (float x = 0; x <= largura; x += 6f * dp) {
            float y = yBase + amplitude * (float) Math.sin(2 * Math.PI * x / comprimentoOnda + fase);
            onda.lineTo(corpo.left + x, y);
        }

        onda.lineTo(corpo.right, corpo.bottom + 2f * dp);
        onda.close();

        canvas.drawPath(onda, paint);
    }

    private void desenhaBolhas(Canvas canvas, float yAgua) {

        float altura = corpo.bottom - yAgua;

        if (altura < 12f * dp) {
            return;
        }

        for (int i = 0; i < QTD_BOLHAS; i++) {

            float progresso = (bolhaFase[i] + faseOnda * bolhaVel[i]) % 1f;
            float y = corpo.bottom - 6f * dp - progresso * (altura - 6f * dp);
            float x = corpo.left + corpo.width() * bolhaX[i] + (float) Math.sin((progresso + bolhaFase[i]) * 9f) * 5f * dp;

            paintBolha.setAlpha((int) (200 * (1f - progresso * 0.7f)));
            canvas.drawCircle(x, y, bolhaRaio[i], paintBolha);
        }
    }

    /** Cano de entrada (cima, à esquerda) e de saída (base, à direita) com gotas em movimento. */
    private void desenhaCanos(Canvas canvas, float nivel) {

        boolean ativo = isEnabled();

        // Entrada: do topo da view até a água
        float xEntrada = corpo.left + corpo.width() * 0.28f;
        float yIni = 0f;
        float yFim = tampa.top + 2f * dp;

        canvas.drawLine(xEntrada, yIni + 4f * dp, xEntrada, yFim, paintCano);

        if (enchendo && ativo) {

            float yAgua = corpo.bottom - corpo.height() * nivel;
            float fim = Math.max(yFim + 20f * dp, Math.min(yAgua, corpo.bottom - 10f * dp));

            paintFluxo.setColor(corDaAgua(Math.max(nivel, 0.5f)));
            paintFluxo.setAlpha(255);

            for (int i = 0; i < 5; i++) {
                float t = (i / 5f + faseFluxo) % 1f;
                float y = yIni + 6f * dp + t * (fim - yIni - 6f * dp);
                canvas.drawCircle(xEntrada, y, 2.6f * dp, paintFluxo);
            }
        }

        // Saída: da lateral direita do tanque para fora
        float ySaida = corpo.bottom - 22f * dp;
        float xIni = corpo.right - 4f * dp;
        float xFim = getWidth() - 4f * dp;

        canvas.drawLine(xIni, ySaida, xFim, ySaida, paintCano);

        if (esvaziando && ativo) {

            paintFluxo.setColor(COR_AGUA);
            paintFluxo.setAlpha(255);

            float inicio = corpo.right - 18f * dp;

            for (int i = 0; i < 4; i++) {
                float t = (i / 4f + faseFluxo * 1.4f) % 1f;
                canvas.drawCircle(inicio + t * (xFim - inicio), ySaida, 2.6f * dp, paintFluxo);
            }
        }
    }
}
