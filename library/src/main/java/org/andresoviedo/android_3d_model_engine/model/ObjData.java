package org.andresoviedo.android_3d_model_engine.model;

import org.andresoviedo.util.core.util.MtlLoaderUtil;

import java.nio.FloatBuffer;
import java.util.ArrayList;

/**
 * @Author: ailibin
 * @Time: 2019/05/14
 * @Description: 3d材质对象
 * @Email: ailibin@qq.com
 */
public class ObjData {

    // 对象名称
    public String name;
    // 材质
    public MtlLoaderUtil.MtlData mtlData;

    /**
     * 顶点坐标
     */
    public int vCount;

    /**
     * 材质漫反射光
     */
    public float[] mDifColor = new float[4];
    /**
     * 材质中alpha
     */
    public float mAlpha;

    /**
     * 顶点、纹理、法向量一一对应后的数据
     */
    public float[] aVertices;
    // 顶点纹理可能会没有
    public float[] aTexCoords;
    public float[] aNormals;

    private FloatBuffer vertsBuffer;
    private FloatBuffer normalsBuffer;

    /**
     * index数组(顶点、纹理、法向量一一对应后，以下三个列表会清空)
     */
    // 顶点index数组
    public ArrayList<Integer> vertexIndices = new ArrayList<Integer>();
    // 纹理index数组
    public ArrayList<Integer> texCoordIndices = new ArrayList<Integer>();
    // 法向量index数组
    public ArrayList<Integer> normalIndices = new ArrayList<Integer>();

    public FloatBuffer getVertsBuffer() {
        return vertsBuffer;
    }

    public void setVertsBuffer(FloatBuffer vertsBuffer) {
        this.vertsBuffer = vertsBuffer;
    }

    public FloatBuffer getNormalsBuffer() {
        return normalsBuffer;
    }

    public void setNormalsBuffer(FloatBuffer normalsBuffer) {
        this.normalsBuffer = normalsBuffer;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public MtlLoaderUtil.MtlData getMtlData() {
        return mtlData;
    }

    public void setMtlData(MtlLoaderUtil.MtlData mtlData) {
        this.mtlData = mtlData;
    }

    public int getvCount() {
        return vCount;
    }

    public void setvCount(int vCount) {
        this.vCount = vCount;
    }

    public float[] getmDifColor() {
        return mDifColor;
    }

    public void setmDifColor(float[] mDifColor) {
        this.mDifColor = mDifColor;
    }

    public float getmAlpha() {
        return mAlpha;
    }

    public void setmAlpha(float mAlpha) {
        this.mAlpha = mAlpha;
    }

    public float[] getaVertices() {
        return aVertices;
    }

    public void setaVertices(float[] aVertices) {
        this.aVertices = aVertices;
    }

    public float[] getaTexCoords() {
        return aTexCoords;
    }

    public void setaTexCoords(float[] aTexCoords) {
        this.aTexCoords = aTexCoords;
    }

    public float[] getaNormals() {
        return aNormals;
    }

    public void setaNormals(float[] aNormals) {
        this.aNormals = aNormals;
    }

    public ArrayList<Integer> getVertexIndices() {
        return vertexIndices;
    }

    public void setVertexIndices(ArrayList<Integer> vertexIndices) {
        this.vertexIndices = vertexIndices;
    }

    public ArrayList<Integer> getTexCoordIndices() {
        return texCoordIndices;
    }

    public void setTexCoordIndices(ArrayList<Integer> texCoordIndices) {
        this.texCoordIndices = texCoordIndices;
    }

    public ArrayList<Integer> getNormalIndices() {
        return normalIndices;
    }

    public void setNormalIndices(ArrayList<Integer> normalIndices) {
        this.normalIndices = normalIndices;
    }
}
